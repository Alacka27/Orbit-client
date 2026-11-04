package dev.alacka.orbit;
import dev.alacka.orbit.menu.ContainerLayout;
import dev.alacka.orbit.menu.PauseLayout;
import dev.alacka.orbit.menu.ProfileListLayout;
public final class GameUiLayoutSelfTest {
    private static int checks;
    private static void check(boolean v,String s){checks++;if(!v)throw new AssertionError(s);}
    public static void main(String[]args){
        for(int sw:new int[]{320,427,640,854,960,1280,1920})for(int sh:new int[]{180,200,240,300,360,540,720,1080}){
            for(int iw:new int[]{176,195,230,276})for(int ih:new int[]{133,166,194,222}){
                int left=(sw-iw)/2,top=(sh-ih)/2;var b=ContainerLayout.of(left,top,iw,ih,sw,sh);
                check(b.x()>=0&&b.y()>=0&&b.x()+b.width()<=sw&&b.y()+b.height()<=sh,"container fits");
                check(b.closes(b.closeX()+5,b.closeY()+5)&&!b.closes(b.closeX()-1,b.closeY()),"close has bounded hitbox");
                if(iw==176&&ih<=sh){
                    for(int row=0;row<3;row++)for(int col=0;col<9;col++){
                        int x=left+8+18*col+8,y=top+18+18*row+8;
                        check(!b.closes(x,y),"close never consumes a native chest slot");
                    }
                }
            }
            for(int count:new int[]{8,9,10,11}){
                var b=PauseLayout.of(sw,sh,count);int rows=3+Math.max(0,(count-3+1)/2);
                check(b.x()>=0&&b.y()>=0&&b.x()+b.width()<=sw&&b.y()+b.height()<=sh,"pause fits");
                check(38+(rows-1)*b.step()+b.buttonHeight()<=b.height()-5,"pause actions fit");
            }
            var p=ProfileListLayout.of(sw,sh);
            check(p.x()>=0&&p.y()>=0&&p.x()+p.width()<=sw&&p.y()+p.height()<=sh,"profiles fits");
            check(p.rowY()+(p.rows()-1)*p.step()+p.rowHeight()<p.editY()-p.y(),"profiles rows avoid editor");
            check(p.editY()+22<p.noticeY()&&p.noticeY()+22<p.footerY(),"profile editor/notice/footer separate");
        }
        System.out.println("PASS: "+checks+" container/pause/profile layout checks; native slot geometry retained.");
    }
}
