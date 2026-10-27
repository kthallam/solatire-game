package com.quietspider.app;
import java.util.*;
public final class SpiderGameTest {
    static int checks;
    static void check(boolean condition,String name) { checks++; if(!condition) throw new AssertionError(name); }
    static SpiderGame.Card card(int rank,int suit) { return new SpiderGame.Card(rank,suit,true); }
    public static void main(String[] args) {
        for(int suits:new int[]{1,2,4}) {
            SpiderGame g=new SpiderGame(suits,42);
            check(g.stock.size()==50,"50 in stock"); int total=g.stock.size();
            Map<String,Integer> counts=new HashMap<>();
            for(List<SpiderGame.Card> a:g.columns) {
                check(a.size()==(g.columns.indexOf(a)<4?6:5),"initial column size");
                for(int i=0;i<a.size();i++) check(a.get(i).up==(i==a.size()-1),"initial face up");
                total+=a.size();
            }
            for(List<SpiderGame.Card> a:g.columns) for(SpiderGame.Card c:a) counts.merge(c.rank+":"+c.suit,1,Integer::sum);
            for(SpiderGame.Card c:g.stock) counts.merge(c.rank+":"+c.suit,1,Integer::sum);
            check(total==104,"104 cards"); check(counts.size()==13*suits,"all ranks and suits");
            for(int n:counts.values()) check(n==8/suits,"correct deck distribution");
            String before=g.encode(); check(g.deal(),"deal succeeds"); check(g.stock.size()==40,"dealt ten");
            check(g.undo()&&g.encode().equals(before),"undo deal exact");
            SpiderGame restored=new SpiderGame(1,0); restored.restore(before); check(restored.encode().equals(before),"save roundtrip");
            try { restored.restore("bad"); throw new AssertionError("accepted bad save"); } catch(IllegalArgumentException expected) { check(restored.encode().equals(before),"corrupt save preserves state"); }
        }
        SpiderGame g=new SpiderGame(2,4); for(List<SpiderGame.Card> a:g.columns) a.clear();
        g.columns.get(0).addAll(Arrays.asList(card(8,0),card(7,0),card(6,0)));
        g.columns.get(1).add(card(9,1));
        check(g.movable(0,0),"same suit stack movable"); check(g.move(0,0,1),"mixed suit destination legal");
        check(!g.movable(1,0),"mixed suit stack cannot move together");
        check(!g.deal(),"cannot deal with empty columns"); check(g.move(1,1,2),"move stack to empty");
        check(!g.move(2,0,2),"reject same column");
        check(!g.canMove(-1,0,2),"reject invalid column");
        for(List<SpiderGame.Card> a:g.columns) a.clear(); g.columns.get(0).add(card(5,1)); g.columns.get(0).get(0).up=false;
        for(int r=13;r>=2;r--) g.columns.get(0).add(card(r,0)); g.columns.get(1).add(card(1,0));
        check(g.move(1,0,0),"complete run"); check(g.completed==1&&g.top(0).rank==5&&g.top(0).up,"clear run and reveal");
        check(g.undo()&&g.completed==0&&g.columns.get(0).size()==13&&!g.columns.get(0).get(0).up,"undo restores completed run and hidden card");
        // Random legal play exercises conservation, save fidelity and exact undo.
        Random random=new Random(90);
        for(int suits:new int[]{1,2,4}) {
            g=new SpiderGame(suits,84);
            for(int turn=0;turn<500;turn++) {
                List<int[]> options=new ArrayList<>();
                for(int c=0;c<10;c++) for(int r=0;r<g.columns.get(c).size();r++) for(int d=0;d<10;d++) if(g.canMove(c,r,d)) options.add(new int[]{c,r,d});
                String before=g.encode(); boolean moved;
                if(g.canDeal()&&(options.isEmpty()||random.nextInt(8)==0)) moved=g.deal();
                else if(!options.isEmpty()) { int[] m=options.get(random.nextInt(options.size())); moved=g.move(m[0],m[1],m[2]); }
                else break;
                check(moved,"legal move succeeds"); String after=g.encode();
                SpiderGame copy=new SpiderGame(1,0); copy.restore(after); check(copy.encode().equals(after),"random game saves");
                check(g.undo()&&g.encode().equals(before),"random undo exact"); g.restore(after);
            }
        }
        System.out.println("Passed "+checks+" rule and state checks.");
    }
}
