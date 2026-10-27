package com.quietspider.app;

import java.util.*;

/** Android-independent Spider rules and reversible game state. */
public final class SpiderGame {
    public static final class Card {
        public final int rank, suit;
        public boolean up;
        Card(int rank, int suit, boolean up) { this.rank=rank; this.suit=suit; this.up=up; }
    }
    public final List<List<Card>> columns = new ArrayList<>();
    public final List<Card> stock = new ArrayList<>();
    private final Deque<String> history = new ArrayDeque<>();
    public int suits, moves, completed, score;
    public SpiderGame(int suits, long seed) { newGame(suits, seed); }
    public void newGame(int count, long seed) {
        if(count!=1 && count!=2 && count!=4) throw new IllegalArgumentException("Suit count");
        suits=count; moves=0; completed=0; score=500; history.clear(); columns.clear(); stock.clear();
        for(int i=0;i<10;i++) columns.add(new ArrayList<>());
        for(int i=0;i<104;i++) stock.add(new Card(i%13+1,(i/13)%suits,false));
        Collections.shuffle(stock,new Random(seed));
        for(int c=0;c<10;c++) {
            for(int r=0;r<(c<4?6:5);r++) columns.get(c).add(stock.remove(stock.size()-1));
            top(c).up=true;
        }
    }
    public Card top(int c) { List<Card> a=columns.get(c); return a.isEmpty()?null:a.get(a.size()-1); }
    public boolean movable(int c,int r) {
        if(c<0||c>=10||r<0||r>=columns.get(c).size()) return false;
        List<Card> a=columns.get(c);
        if(!a.get(r).up) return false;
        for(int i=r+1;i<a.size();i++) if(!a.get(i).up||a.get(i-1).rank!=a.get(i).rank+1||a.get(i-1).suit!=a.get(i).suit) return false;
        return true;
    }
    public boolean canMove(int c,int r,int dest) {
        if(dest<0||dest>=10||c==dest||!movable(c,r)) return false;
        Card t=top(dest); return t==null||t.rank==columns.get(c).get(r).rank+1;
    }
    private void remember() { history.push(encode()); if(history.size()>200) history.removeLast(); }
    public boolean move(int c,int r,int dest) {
        if(!canMove(c,r,dest)) return false;
        remember(); List<Card> a=columns.get(c); columns.get(dest).addAll(new ArrayList<>(a.subList(r,a.size()))); a.subList(r,a.size()).clear();
        if(top(c)!=null) top(c).up=true;
        moves++; score--; collect(); return true;
    }
    public boolean canDeal() { if(stock.isEmpty()) return false; for(List<Card> a:columns) if(a.isEmpty()) return false; return true; }
    public boolean deal() {
        if(!canDeal()) return false;
        remember(); for(int i=0;i<10;i++) { Card c=stock.remove(stock.size()-1); c.up=true; columns.get(i).add(c); }
        moves++; score--; collect(); return true;
    }
    private void collect() {
        for(List<Card> a:columns) {
            boolean again=true;
            while(again && a.size()>=13) {
                again=false; int start=a.size()-13; int suit=a.get(start).suit; boolean ok=true;
                for(int i=0;i<13;i++) { Card c=a.get(start+i); if(!c.up||c.rank!=13-i||c.suit!=suit) ok=false; }
                if(ok) { a.subList(start,a.size()).clear(); completed++; score+=100; if(!a.isEmpty()) a.get(a.size()-1).up=true; again=true; }
            }
        }
    }
    public int[] hint() {
        for(int c=0;c<10;c++) for(int r=0;r<columns.get(c).size();r++) for(int d=0;d<10;d++) {
            if(canMove(c,r,d) && !(r==0 && columns.get(d).isEmpty())) return new int[]{c,r,d};
        }
        return null;
    }
    public boolean undo() { if(history.isEmpty()) return false; decode(history.pop()); return true; }
    public String encode() {
        StringBuilder s=new StringBuilder("1,").append(suits).append(',').append(moves).append(',').append(completed).append(',').append(score);
        for(List<Card> a:columns) append(s,a); append(s,stock); return s.toString();
    }
    private void append(StringBuilder s,List<Card> a) { s.append('|'); for(Card c:a) s.append(c.rank).append(':').append(c.suit).append(':').append(c.up?1:0).append(';'); }
    public void restore(String state) {
        // Parse into a separate game first, so a corrupt save cannot damage this game.
        SpiderGame candidate=new SpiderGame(1,0); candidate.decode(state);
        int total=candidate.stock.size()+candidate.completed*13;
        for(List<Card> a:candidate.columns) total+=a.size();
        if(total!=104||candidate.stock.size()%10!=0||candidate.stock.size()>50||candidate.completed<0||candidate.completed>8) throw new IllegalArgumentException("Invalid save");
        decode(state); history.clear();
    }
    private void decode(String state) {
        String[] parts=state.split("\\|",-1); if(parts.length!=12) throw new IllegalArgumentException("Invalid save");
        String[] h=parts[0].split(","); if(h.length!=5||!h[0].equals("1")) throw new IllegalArgumentException("Save version");
        int n=Integer.parseInt(h[1]); if(n!=1&&n!=2&&n!=4) throw new IllegalArgumentException("Suits");
        suits=n; moves=Integer.parseInt(h[2]); completed=Integer.parseInt(h[3]); score=Integer.parseInt(h[4]);
        columns.clear(); stock.clear();
        for(int i=1;i<12;i++) {
            List<Card> a=i==11?stock:new ArrayList<>();
            if(!parts[i].isEmpty()) for(String token:parts[i].split(";")) {
                String[] v=token.split(":"); if(v.length!=3) throw new IllegalArgumentException("Card");
                int rank=Integer.parseInt(v[0]),suit=Integer.parseInt(v[1]);
                if(rank<1||rank>13||suit<0||suit>=suits||(!v[2].equals("0")&&!v[2].equals("1"))) throw new IllegalArgumentException("Card");
                a.add(new Card(rank,suit,v[2].equals("1")));
            }
            if(i<11) columns.add(a);
        }
    }
}
