package Fusion;

import HAL.GridsAndAgents.AgentSQ2Dunstackable;
import HAL.Gui.GridWindow;
import HAL.GridsAndAgents.AgentGrid2D;
import HAL.Rand;
import HAL.Util;

import java.util.Collections;
import java.util.Dictionary;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static HAL.Util.*;

/**
 * Created by Paulameena 5/8/2024, adapted from HAL documentation
 */

class Cell extends AgentSQ2Dunstackable<SimpleFusion> {
    int color; //parental = green; resistant = red; fused = yellow (to match fluorescence we saw in FACS)
    String cell_type; // parental, resistant, or fused
    double DEATH_PROB = 0.01;
    double BIRTH_PROB = 0.2;
    double FUSE_PROB = 0.0001;
    //int[] genotype;

    public void Step() {
        if (G.rn.Double() < this.DEATH_PROB) {
            Dispose();
            return;
        }
        if (G.rn.Double() < this.FUSE_PROB) {
            Fuse();
            return;
        }
        if (G.rn.Double() < this.BIRTH_PROB) {
            int nOptions = G.MapEmptyHood(G.mooreHood, Xsq(), Ysq());
            if(nOptions>0) {
                Cell child = G.NewAgentSQ(G.mooreHood[G.rn.Int(nOptions)]);
                child.color=this.color;
                child.cell_type = this.cell_type;
                //TODO: once genetic information is encoded, will need to allow for parasexual-style genetic mixing in the children if cell type is fused :0
            }
        }
    }

    private void Fuse() {
        int nOptions = G.MapOccupiedHood(G.mooreHood, Xsq(), Ysq());
        String partner_type = null;
        Cell fuse_partner = null;
        for (int i = 0; i < nOptions; i++) {
            if (partner_type != null) {
                break;
            }
            fuse_partner = G.GetAgent(G.mooreHood[G.rn.Int(nOptions)]);
            String test = fuse_partner.cell_type;
            if ((test.equals("parental")) | (test.equals("resistant"))) { //only allowed to merge if the neighbor is also not a fused cell //TODO: change to allow for multiple fusions
                partner_type = test;
            }
        }
        if (partner_type != null) {
            fuse_partner.Dispose();
            if ((!this.cell_type.equals(partner_type))) {
                this.color = Util.YELLOW;
            }
            this.cell_type = "fused";
            this.FUSE_PROB = 0; //ONCE FUSED, NO MORE FUSION allowed (for now //TODO change later)
            //TODO: change color only if parental is merging with resistant, but cell_type label should change regardless
            }
        }
    }


public class SimpleFusion extends AgentGrid2D<Cell> {
    int BLACK= Util.RGB(0,0,0);
    Rand rn=new Rand();
    int[]mooreHood= Util.MooreHood(false);
    //int color;
    public SimpleFusion(int x, int y) {
        super(x, y, Cell.class);
        //this.color=color;
    }
    public void Setup(double rad){
        int[]coords= Util.CircleHood(true,rad);
        int nCoords= MapHood(coords,xDim/2,yDim/2);
        for (int i = 0; i < nCoords ; i++) {
            //todo: define original population ratio as input
            Cell new_cell = NewAgentSQ(coords[i]);
            if (rn.Double() <= 0.5) {
                new_cell.color=Util.RED;
                new_cell.cell_type = "resistant";
            }
            else {
                new_cell.color = Util.GREEN;
                new_cell.cell_type = "parental";
            }
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
        SimpleFusion t=new SimpleFusion(100,100);
        GridWindow win=new GridWindow(100,100,10);
        t.Setup(10);
        for (int i = 0; i < 100000; i++) {
            win.TickPause(10);
            t.Step();
            t.Draw(win);
        }
    }
}
