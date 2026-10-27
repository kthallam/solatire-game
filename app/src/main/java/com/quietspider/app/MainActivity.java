package com.quietspider.app;

import android.app.*;
import android.os.Bundle;
import android.content.*;
import android.graphics.*;
import android.view.*;
import android.widget.Toast;
import java.util.List;

public final class MainActivity extends Activity {
    private SpiderGame game;
    private Table table;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(12,32,30));
        getWindow().setNavigationBarColor(Color.rgb(12,32,30));
        game=new SpiderGame(1,System.nanoTime());
        String saved=getPreferences(0).getString("game",null);
        if(saved!=null) try { game.restore(saved); } catch(RuntimeException e) { toast("The saved game could not be read. A new game is ready."); }
        table=new Table();
        android.widget.FrameLayout root=new android.widget.FrameLayout(this);
        root.setBackgroundColor(Color.rgb(12,32,30));
        root.addView(table,new android.widget.FrameLayout.LayoutParams(-1,-1));
        root.setOnApplyWindowInsetsListener((view,insets)-> {
            view.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());
            return insets;
        });
        setContentView(root); root.requestApplyInsets();
    }
    private void save() { getPreferences(0).edit().putString("game",game.encode()).apply(); }
    @Override protected void onPause() { super.onPause(); save(); }
    private void toast(String s) { Toast.makeText(this,s,Toast.LENGTH_SHORT).show(); }
    private void newGame() {
        new AlertDialog.Builder(this).setTitle("Start a new game?").setMessage("Your current game will be replaced.")
            .setNegativeButton("Keep playing",null).setPositiveButton("Choose difficulty",(d,w)->
                new AlertDialog.Builder(this).setTitle("Choose your challenge").setItems(new String[]{"1 suit · Relaxed","2 suits · Balanced","4 suits · Expert"},(dialog,which)-> {
                    game.newGame(new int[]{1,2,4}[which],System.nanoTime()); table.clear(); save(); table.invalidate();
                }).show()).show();
    }
    private void help() {
        new AlertDialog.Builder(this).setTitle("A little room to unwind")
            .setMessage("Move cards onto a card one rank higher, regardless of suit. Tap a face-up card, then a destination column. A sequence moves together only when its cards descend in the same suit.\n\nBuild King through Ace in one suit to clear a run. Clear all eight to win. Any card or valid sequence can fill an empty column.\n\nDeal adds one card to every column; fill empty columns first. Undo reverses moves and deals. Games save automatically.\n\nNo ads. No accounts. No internet needed.")
            .setPositiveButton("Let's play",null).show();
    }
    private final class Table extends View {
        final Paint p=new Paint(3);
        Shader background;
        int selected=-1,row=-1,hintDest=-1;
        float unit,cw,ch,startY,step,margin;
        final String[] symbols={"♠","♥","♣","♦"};
        final String[] ranks={"","A","2","3","4","5","6","7","8","9","10","J","Q","K"};
        Table() { super(MainActivity.this); setLayerType(View.LAYER_TYPE_SOFTWARE,null); setContentDescription("Spider Solitaire table. Tap a card, then its destination. Use Help for rules."); }
        void clear() { selected=row=hintDest=-1; }
        int color(String s) { return Color.parseColor(s); }
        void text(Canvas c,String s,float x,float y,float size,int col,boolean bold) { p.setColor(col); p.setTextSize(size); p.setTypeface(bold?Typeface.create("sans-serif",Typeface.BOLD):Typeface.create("sans-serif",Typeface.NORMAL)); c.drawText(s,x,y,p); }
        void box(Canvas c,float x,float y,float w,float h,int col,float radius) { p.setColor(col); c.drawRoundRect(x,y,x+w,y+h,radius,radius,p); }
        @Override protected void onSizeChanged(int w,int h,int oldW,int oldH) {
            background=new LinearGradient(0,0,w,h,new int[]{color("#163f36"),color("#0b2523")},null,Shader.TileMode.CLAMP);
        }
        @Override protected void onDraw(Canvas c) {
            super.onDraw(c); float w=getWidth(),h=getHeight(); unit=Math.min(w/420f,h/650f); margin=10*unit;
            p.setShader(background); c.drawRect(0,0,w,h,p); p.setShader(null);
            text(c,"QUIET SPIDER",16*unit,32*unit,20*unit,color("#f4e9cf"),true);
            text(c,game.suits+" SUIT"+(game.suits>1?"S":"")+"  /  "+game.moves+" MOVES  /  "+game.score+" POINTS",16*unit,54*unit,10*unit,color("#a5bdb1"),false);
            text(c,game.completed+" / 8 RUNS",w-91*unit,32*unit,12*unit,color("#d8c18f"),true);
            float buttonW=(w-2*margin)/5;
            String[] labels={"New","Undo","Hint","Deal "+game.stock.size()/10,"Help"};
            for(int i=0;i<5;i++) {
                box(c,margin+i*buttonW,70*unit,buttonW-5*unit,36*unit,color(i==3?"#d8c18f":"#284c42"),7*unit);
                p.setTextAlign(Paint.Align.CENTER); text(c,labels[i],margin+(i+.5f)*buttonW-2*unit,93*unit,12*unit,color(i==3?"#17382f":"#eee5d1"),true); p.setTextAlign(Paint.Align.LEFT);
            }
            startY=134*unit; cw=(w-2*margin)/10; ch=cw*1.47f;
            int max=1; for(List<SpiderGame.Card> a:game.columns) max=Math.max(max,a.size());
            step=Math.max(3*unit,Math.min(cw*.40f,(h-startY-ch-42*unit)/Math.max(1,max-1)));
            for(int col=0;col<10;col++) {
                float x=margin+col*cw;
                box(c,x+2*unit,startY,cw-4*unit,ch,color(col==hintDest?"#a38a51":"#244b40"),4*unit);
                List<SpiderGame.Card> a=game.columns.get(col);
                for(int r=0;r<a.size();r++) {
                    SpiderGame.Card card=a.get(r); float y=startY+r*step;
                    boolean chosen=col==selected&&r>=row;
                    box(c,x+2*unit,y+unit,cw-4*unit,ch,color(chosen?"#edcc7a":card.up?"#f5f0e4":"#2f6960"),3*unit);
                    if(card.up) {
                        int ink=color(card.suit==1||card.suit==3?"#a6373b":"#1c3431");
                        text(c,ranks[card.rank]+symbols[card.suit],x+4*unit,y+cw*.32f,cw*.27f,ink,true);
                        if(r==a.size()-1) { p.setTextAlign(Paint.Align.CENTER); text(c,symbols[card.suit],x+cw/2,y+ch*.77f,cw*.55f,ink,false); p.setTextAlign(Paint.Align.LEFT); }
                    } else {
                        p.setColor(color("#4d887d")); p.setStyle(Paint.Style.STROKE); c.drawRoundRect(x+5*unit,y+3*unit,x+cw-5*unit,y+ch-3*unit,2*unit,2*unit,p); p.setStyle(Paint.Style.FILL);
                    }
                }
            }
            p.setTextAlign(Paint.Align.CENTER);
            text(c,game.completed==8?"Beautifully done. All eight runs cleared.":selected>=0?"Tap a destination column":"Take your time. This moment is yours.",w/2,h-17*unit,12*unit,color("#a5bdb1"),false);
            p.setTextAlign(Paint.Align.LEFT);
        }
        @Override public boolean onTouchEvent(android.view.MotionEvent e) {
            if(e.getAction()!=MotionEvent.ACTION_UP) return true;
            performClick(); float x=e.getX(),y=e.getY();
            if(y>=70*unit&&y<=106*unit) {
                int button=(int)((x-margin)/((getWidth()-2*margin)/5));
                if(button==0) newGame();
                if(button==1) { if(!game.undo()) toast("No moves to undo."); clear(); }
                if(button==2) { int[] hint=game.hint(); if(hint==null) toast(game.canDeal()?"Try dealing a new row.":"No move found. Try undo or a new game."); else { selected=hint[0]; row=hint[1]; hintDest=hint[2]; toast("Move the highlighted cards to column "+(hintDest+1)+"."); } }
                if(button==3) { if(!game.deal()) toast(game.stock.isEmpty()?"All stock cards have been dealt.":"Fill every empty column before dealing."); clear(); }
                if(button==4) help();
                save(); invalidate(); return true;
            }
            if(y<startY||x<margin||x>=getWidth()-margin) return true;
            int col=(int)((x-margin)/cw); List<SpiderGame.Card> a=game.columns.get(col);
            if(selected>=0&&game.move(selected,row,col)) { clear(); save(); if(game.completed==8) new AlertDialog.Builder(MainActivity.this).setTitle("A quiet victory").setMessage("All eight runs cleared in "+game.moves+" moves. Nicely played.").setPositiveButton("Enjoy the moment",null).show(); }
            else {
                int r=a.isEmpty()?-1:Math.min(a.size()-1,(int)((y-startY)/step));
                if(r>=0&&y<=startY+(a.size()-1)*step+ch&&game.movable(col,r)) { if(selected==col&&row==r) clear(); else { selected=col; row=r; hintDest=-1; } }
                else { clear(); toast("Select a descending sequence in the same suit."); }
            }
            invalidate(); return true;
        }
        @Override public boolean performClick() { super.performClick(); return true; }
    }
}
