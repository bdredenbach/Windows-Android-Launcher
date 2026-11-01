package com.nthexperience.desktop;

import org.json.*;
import java.util.*;

/** A saved 4 by 4 sliding puzzle, shuffled only by legal moves from the solved board. */
final class TilePuzzle {
    static int[] board(JSONObject data){int[] board=new int[16];JSONArray saved=data.optJSONArray("board");for(int i=0;i<16;i++)board[i]=saved!=null&&saved.length()==16?saved.optInt(i):(i+1)%16;return board;}
    static boolean move(int[] board,int position){if(position<0||position>=16||board[position]==0)return false;int empty=0;while(empty<16&&board[empty]!=0)empty++;if(empty==16||Math.abs(empty/4-position/4)+Math.abs(empty%4-position%4)!=1)return false;board[empty]=board[position];board[position]=0;return true;}
    static boolean solved(int[] board){for(int i=0;i<16;i++)if(board[i]!=(i+1)%16)return false;return true;}
    static int[] shuffle(Random random){int[] board=board(new JSONObject());int last=-1;for(int i=0;i<240;i++){int empty=0;while(board[empty]!=0)empty++;List<Integer> choices=new ArrayList<>();for(int j=0;j<16;j++)if(j!=last&&Math.abs(empty/4-j/4)+Math.abs(empty%4-j%4)==1)choices.add(j);int next=choices.get(random.nextInt(choices.size()));last=empty;move(board,next);}if(solved(board))move(board,14);return board;}
    static void save(DesktopStore store,int[] board,int moves){JSONObject data=new JSONObject();JSONArray values=new JSONArray();for(int tile:board)values.put(tile);GadgetLayout.put(data,"board",values);GadgetLayout.put(data,"moves",moves);GadgetData.save(store,"puzzle",data);}
    static void validate(JSONObject data)throws JSONException {if(!data.has("board"))return;JSONArray board=data.getJSONArray("board");if(board.length()!=16)throw new JSONException("Puzzle requires sixteen cells");boolean[] seen=new boolean[16];int inversions=0,empty=0;for(int i=0;i<16;i++){Object value=board.get(i);if(!(value instanceof Number)||board.getDouble(i)!=board.getInt(i))throw new JSONException("Invalid puzzle tile");int tile=board.getInt(i);if(tile<0||tile>15||seen[tile])throw new JSONException("Invalid puzzle board");seen[tile]=true;if(tile==0)empty=i;else for(int j=0;j<i;j++)if(board.getInt(j)>tile)inversions++;}if((inversions+4-empty/4)%2!=1||data.optInt("moves",0)<0)throw new JSONException("Unsolvable puzzle board");}
}
