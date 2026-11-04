package dev.alacka.orbit.config;

import java.io.IOException;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

public final class ConfigStore {
   public static final String EXTENSION = ".orbit";
   public static final int MAX_NAME = 48;
   public static final long MAX_BYTES = 65536L;
   public static final String FORBIDDEN = "\\/:*?\"<>|";
   private static final Set<String> RESERVED = Set.of(
      "CON",
      "PRN",
      "AUX",
      "NUL",
      "COM1",
      "COM2",
      "COM3",
      "COM4",
      "COM5",
      "COM6",
      "COM7",
      "COM8",
      "COM9",
      "LPT1",
      "LPT2",
      "LPT3",
      "LPT4",
      "LPT5",
      "LPT6",
      "LPT7",
      "LPT8",
      "LPT9"
   );

   private ConfigStore() {
   }

   public static boolean allowed(int codePoint) {
      return codePoint >= 32
         && codePoint != 127
         && (codePoint < 128 || codePoint >= 160)
         && (codePoint < 55296 || codePoint > 57343)
         && "\\/:*?\"<>|".indexOf(codePoint) < 0;
   }

   public static String sanitize(String raw) {
      if (raw == null) {
         return "";
      } else {
         StringBuilder out = new StringBuilder();
         raw.codePoints().forEach(cp -> {
            if (Character.isWhitespace(cp) || Character.isSpaceChar(cp)) {
               out.append(' ');
            } else if (allowed(cp)) {
               out.appendCodePoint(cp);
            }
         });
         String s = out.toString().replaceAll(" {2,}", " ").strip();
         if (s.toLowerCase(Locale.ROOT).endsWith(".orbit")) {
            s = s.substring(0, s.length() - ".orbit".length());
         }

         while (s.startsWith(".")) {
            s = s.substring(1).stripLeading();
         }

         s = limit(s);
         if (s.isEmpty()) {
            return "";
         } else {
            int dot = s.indexOf(46);
            String base = (dot < 0 ? s : s.substring(0, dot)).strip();
            if (RESERVED.contains(base.toUpperCase(Locale.ROOT))) {
               s = limit(base + "_" + (dot < 0 ? "" : s.substring(dot)));
            }

            return s;
         }
      }
   }

   private static String limit(String s) {
      if (s.codePointCount(0, s.length()) > 48) {
         s = s.substring(0, s.offsetByCodePoints(0, 48));
      }

      int end = s.length();

      while (end > 0 && (s.charAt(end - 1) == '.' || s.charAt(end - 1) == ' ')) {
         end--;
      }

      return s.substring(0, end);
   }

   public static Path file(Path dir, String name) {
      return dir.resolve(sanitize(name) + ".orbit");
   }

   public static List<ConfigStore.Entry> list(Path dir) throws IOException {
      if (dir != null && Files.isDirectory(dir)) {
         List<ConfigStore.Entry> out = new ArrayList<>();

         try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path file : stream) {
               String fileName = file.getFileName().toString();
               if (!fileName.startsWith(".") && fileName.length() > ".orbit".length() && fileName.toLowerCase(Locale.ROOT).endsWith(".orbit")) {
                  BasicFileAttributes attrs;
                  try {
                     attrs = Files.readAttributes(file, BasicFileAttributes.class);
                  } catch (IOException var9) {
                     continue;
                  }

                  if (attrs.isRegularFile()) {
                     out.add(
                        new ConfigStore.Entry(
                           fileName.substring(0, fileName.length() - ".orbit".length()), file, attrs.lastModifiedTime().toMillis(), attrs.size()
                        )
                     );
                  }
               }
            }
         }

         out.sort(Comparator.comparing(ConfigStore.Entry::name, String.CASE_INSENSITIVE_ORDER).thenComparing(ConfigStore.Entry::name));
         return out;
      } else {
         return List.of();
      }
   }

   public static ConfigStore.Entry find(List<ConfigStore.Entry> entries, String name) {
      String clean = sanitize(name);

      for (ConfigStore.Entry entry : entries) {
         if (entry.name().equalsIgnoreCase(clean)) {
            return entry;
         }
      }

      return null;
   }

   public static OrbitConfig read(Path file) throws IOException {
      if (Files.size(file) > 65536L) {
         throw new IOException("Config is larger than 64 KB");
      } else {
         byte[] bytes = Files.readAllBytes(file);
         if (bytes.length > 65536L) {
            throw new IOException("Config is larger than 64 KB");
         } else {
            String text;
            try {
               text = StandardCharsets.UTF_8
                  .newDecoder()
                  .onMalformedInput(CodingErrorAction.REPORT)
                  .onUnmappableCharacter(CodingErrorAction.REPORT)
                  .decode(ByteBuffer.wrap(bytes))
                  .toString();
            } catch (CharacterCodingException var6) {
               throw new IOException("Config is not UTF-8 text", var6);
            }

            if (text.startsWith("\ufeff")) {
               text = text.substring(1);
            }

            Properties p = new Properties();

            try {
               p.load(new StringReader(text));
               return OrbitConfig.fromProperties(p);
            } catch (IllegalArgumentException var5) {
               throw new IOException("Invalid Orbit config: " + var5.getMessage(), var5);
            }
         }
      }
   }

   public static Path create(Path dir, String name, OrbitConfig config, String version) throws IOException {
      String clean = sanitize(name);
      if (clean.isEmpty()) {
         throw new IllegalArgumentException("name");
      } else {
         Files.createDirectories(dir);
         Path file = dir.resolve(clean + ".orbit");
         if (!Files.exists(file) && find(list(dir), clean) == null) {
            ClientSettings.store(file, config.toProperties(version), "Orbit config \"" + clean + "\". Contains no account data.");
            return file;
         } else {
            throw new FileAlreadyExistsException(file.toString());
         }
      }
   }

   public static void overwrite(Path file, String name, OrbitConfig config, String version) throws IOException {
      ClientSettings.store(file, config.toProperties(version), "Orbit config \"" + name + "\". Contains no account data.");
   }

   private static void copy(Path source, Path target) throws IOException {
      Path tmp = Files.createTempFile(target.toAbsolutePath().getParent(), ".orbit-import-", ".tmp");

      try {
         Files.copy(source, tmp, StandardCopyOption.REPLACE_EXISTING);
         Files.move(tmp, target);
      } finally {
         Files.deleteIfExists(tmp);
      }
   }

   public static void delete(Path file) throws IOException {
      Files.deleteIfExists(file);
   }

   public static String importFile(Path dir, Path source) throws IOException {
      String fileName = source.getFileName() == null ? "" : source.getFileName().toString();
      if (fileName.toLowerCase(Locale.ROOT).endsWith(".orbit") && Files.isRegularFile(source)) {
         read(source);
         String base = sanitize(fileName);
         if (base.isEmpty()) {
            base = "Config";
         }

         Files.createDirectories(dir);
         Path parent = source.toAbsolutePath().normalize().getParent();
         if (parent != null && parent.equals(dir.toAbsolutePath().normalize())) {
            return fileName.substring(0, fileName.length() - ".orbit".length());
         } else {
            String name = base;
            List<ConfigStore.Entry> existing = list(dir);

            for (int i = 2; find(existing, name) != null; i++) {
               String suffix = " (" + i + ")";
               name = limit(base.substring(0, base.offsetByCodePoints(0, Math.min(base.codePointCount(0, base.length()), 48 - suffix.length())))) + suffix;
            }

            copy(source, dir.resolve(name + ".orbit"));
            return name;
         }
      } else {
         return null;
      }
   }

   public record Entry(String name, Path file, long modified, long size) {
   }
}
