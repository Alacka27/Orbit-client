package dev.alacka.orbit.config;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DesktopLocator {
   public static final String FOLDER = "Orbit Configs";
   private static final Pattern REGISTRY = Pattern.compile("(?im)^\\s*Desktop\\s+REG_(?:EXPAND_)?SZ\\s+(.+?)\\s*$");
   private static final Pattern WINDOWS_VAR = Pattern.compile("%([^%]+)%");

   private DesktopLocator() {
   }

   public static Path desktop(DesktopLocator.Host host) {
      String os = host.os() == null ? "" : host.os().toLowerCase(Locale.ROOT);
      List<Supplier<List<String>>> sources = new ArrayList<>();
      if (os.startsWith("windows")) {
         sources.add(() -> single(host.knownFolder()));
         sources.add(() -> {
            List<String> values = new ArrayList<>();

            for (String output : host.registry()) {
               if (output != null) {
                  Matcher m = REGISTRY.matcher(output);
                  if (m.find()) {
                     values.add(expandWindows(m.group(1), host));
                  }
               }
            }

            return values;
         });
         sources.add(() -> single(joinWindows(host.env("OneDrive"), "Desktop")));
         sources.add(() -> single(joinWindows(host.env("USERPROFILE"), "Desktop")));
      } else if (!os.contains("mac") && !os.contains("darwin")) {
         sources.add(() -> single(xdgDesktop(host.xdgUserDirs(), host.home())));
      }

      sources.add(() -> single(host.home() == null ? null : host.home().resolve("Desktop").toString()));

      for (Supplier<List<String>> source : sources) {
         for (String candidate : source.get()) {
            if (candidate != null && !candidate.isBlank()) {
               Path path = host.path(candidate.strip());
               if (path != null && host.isDirectory(path)) {
                  return path;
               }
            }
         }
      }

      return null;
   }

   private static List<String> single(String value) {
      return value == null ? List.of() : List.of(value);
   }

   private static String joinWindows(String base, String child) {
      if (base != null && !base.isBlank()) {
         return base.endsWith("\\") ? base + child : base + "\\" + child;
      } else {
         return null;
      }
   }

   public static Path folder(DesktopLocator.Host host, Path fallback) {
      Path desktop = desktop(host);
      return desktop == null ? fallback : desktop.resolve("Orbit Configs");
   }

   static String expandWindows(String value, DesktopLocator.Host host) {
      Matcher m = WINDOWS_VAR.matcher(value);
      StringBuilder out = new StringBuilder();

      while (m.find()) {
         String replacement = host.env(m.group(1));
         if (replacement == null) {
            return null;
         }

         m.appendReplacement(out, Matcher.quoteReplacement(replacement));
      }

      m.appendTail(out);
      return out.toString();
   }

   static String xdgDesktop(String content, Path home) {
      if (content == null) {
         return null;
      } else {
         for (String line : content.split("\\R")) {
            String s = line.strip();
            if (s.startsWith("XDG_DESKTOP_DIR=")) {
               String value = s.substring("XDG_DESKTOP_DIR=".length()).strip();
               if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
                  value = value.substring(1, value.length() - 1);
               }

               value = value.replace("\\\"", "\"").replace("\\\\", "\\");
               if (!value.equals("$HOME") && !value.equals("$HOME/") && !value.equals("${HOME}")) {
                  if (value.startsWith("$HOME/")) {
                     return home == null ? null : home.resolve(value.substring(6)).toString();
                  }

                  if (value.startsWith("${HOME}/")) {
                     return home == null ? null : home.resolve(value.substring(8)).toString();
                  }

                  return value.startsWith("/") ? value : null;
               }

               return home == null ? null : home.toString();
            }
         }

         return null;
      }
   }

   public static DesktopLocator.Host system() {
      return new DesktopLocator.Host() {
         @Override
         public String os() {
            return System.getProperty("os.name", "");
         }

         @Override
         public String env(String name) {
            return System.getenv(name);
         }

         @Override
         public Path home() {
            String home = System.getProperty("user.home");
            return home != null && !home.isBlank() ? Path.of(home) : null;
         }

         @Override
         public String knownFolder() {
            try {
               Class<?> folders = Class.forName("com.sun.jna.platform.win32.KnownFolders");
               Object id = folders.getField("FOLDERID_Desktop").get(null);
               Class<?> guid = Class.forName("com.sun.jna.platform.win32.Guid$GUID");
               return Class.forName("com.sun.jna.platform.win32.Shell32Util").getMethod("getKnownFolderPath", guid).invoke(null, id) instanceof String s
                  ? s
                  : null;
            } catch (Throwable var6) {
               return null;
            }
         }

         @Override
         public List<String> registry() {
            try {
               Process process = new ProcessBuilder(
                     "reg", "query", "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\User Shell Folders", "/v", "Desktop"
                  )
                  .redirectErrorStream(true)
                  .start();
               if (!process.waitFor(3L, TimeUnit.SECONDS)) {
                  process.destroyForcibly();
                  return List.of();
               } else {
                  byte[] output;
                  try (InputStream in = process.getInputStream()) {
                     output = in.readNBytes(16384);
                  }

                  LinkedHashSet<String> decoded = new LinkedHashSet<>();

                  for (String name : new String[]{"IBM866", System.getProperty("native.encoding", "windows-1251"), "UTF-8"}) {
                     try {
                        decoded.add(new String(output, Charset.forName(name)));
                     } catch (Exception var10) {
                     }
                  }

                  decoded.add(new String(output, StandardCharsets.UTF_8));
                  return List.copyOf(decoded);
               }
            } catch (Exception var12) {
               return List.of();
            }
         }

         @Override
         public String xdgUserDirs() {
            try {
               String config = System.getenv("XDG_CONFIG_HOME");
               Path base = config != null && !config.isBlank() ? Path.of(config) : (this.home() == null ? null : this.home().resolve(".config"));
               Path file = base == null ? null : base.resolve("user-dirs.dirs");
               return file != null && Files.isRegularFile(file) ? Files.readString(file, StandardCharsets.UTF_8) : null;
            } catch (Exception var4) {
               return null;
            }
         }

         @Override
         public boolean isDirectory(Path path) {
            try {
               return Files.isDirectory(path);
            } catch (Exception var3) {
               return false;
            }
         }

         @Override
         public Path path(String value) {
            try {
               return Path.of(value);
            } catch (Exception var3) {
               return null;
            }
         }
      };
   }

   public interface Host {
      String os();

      String env(String var1);

      Path home();

      String knownFolder();

      List<String> registry();

      String xdgUserDirs();

      boolean isDirectory(Path var1);

      Path path(String var1);
   }
}
