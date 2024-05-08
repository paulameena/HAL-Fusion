package Fusion;

import HAL.GridsAndAgents.AgentSQ2Dunstackable;
import HAL.Gui.GridWindow;
import HAL.GridsAndAgents.AgentGrid2D;
import HAL.Rand;
import HAL.Util;

import static HAL.Util.*;

/**
 * Created by Paulameena 5/8/2024, adapted from HAL documentation
 */

class Cell extends AgentSQ2Dunstackable<SimpleFusion> {
    int color;
    String cell_type; // parental, resistant, or fused
    double DEATH_PROB;
    double BIRTH_PROB;
    double FUSE_PROB;
    //int[] genotype;

    public void Step() {
        if (G.rn.Double() < this.DEATH_PROB) {
            Dispose();
            return;
        }
        if (G.rn.Double() < this.FUSE_PROB) {
            Fuse();
        }
        if (G.rn.Double() < this.BIRTH_PROB) {
            int nOptions = G.MapEmptyHood(G.mooreHood, Xsq(), Ysq());
            if(nOptions>0) {
                G.NewAgentSQ(G.mooreHood[G.rn.Int(nOptions)]).color=color;
            }
        }
    }

    private void Fuse() {
        int nOptions = G.MapOccupiedHood(G.mooreHood, Xsq(), Ysq());
        if(nOptions >0) {
            Cell fuse_partner = G.GetAgent(G.rn.Int(nOptions));
        }
    }
}

public class SimpleFusion extends AgentGrid2D<Cell> {
    int BLACK= Util.RGB(0,0,0);
    Rand rn=new Rand();
    int[]mooreHood= Util.MooreHood(false);
    int color;
    public SimpleFusion(int x, int y,int color) {
        super(x, y, Cell.class);
        this.color=color;
    }
    public void Setup(double rad){
        int[]coords= Util.CircleHood(true,rad);
        int nCoords= MapHood(coords,xDim/2,yDim/2);
        for (int i = 0; i < nCoords ; i++) {
            NewAgentSQ(coords[i]).color=color;
        }
    }
    public void Step() {
        for (Cell c : this) {
            c.Step();
        }
        CleanAgents();
        ShuffleAgents(rn);
    }
    public void Draw(GridWindow vis){
        for (int i = 0; i < vis.length; i++) {
            Cell c = GetAgent(i);
            vis.SetPix(i, c == null ? BLACK : c.color);
        }
    }


    public static void main(String[] args) {
        SimpleFusion t=new SimpleFusion(100,100, Util.RED);
        GridWindow win=new GridWindow(100,100,10);
        t.Setup(10);
        for (int i = 0; i < 100000; i++) {
            win.TickPause(10);
            t.Step();
            t.Draw(win);
        }
    }
}
