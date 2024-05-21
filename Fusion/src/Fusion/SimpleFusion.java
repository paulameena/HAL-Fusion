package Fusion;

import HAL.GridsAndAgents.AgentSQ2Dunstackable;
import HAL.Gui.GifMaker;
import HAL.Gui.GridWindow;
import HAL.GridsAndAgents.AgentGrid2D;
import HAL.Rand;
import HAL.Tools.FileIO;
import HAL.Util;

import javax.xml.stream.events.EndDocument;
import java.util.Collections;
import java.util.Date;
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
    double DEATH_PROB;
    double BIRTH_PROB;
    double FUSE_PROB;
    //int[] genotype;

    public void UpdateCellCounts(boolean function) { //function: False if remove, True if add
        if (cell_type.equals("resistant")) {
            if (!function) {
                G.resistant_cell_count--;
            } else {
                G.resistant_cell_count++;
            }
        } else if (cell_type.equals("parental")) {
            if (!function) {
                G.parental_cell_count--;
            } else {
                G.parental_cell_count++;
            }
        } else {
            if (!function) {
                G.fused_cell_count--;
            } else {
                G.fused_cell_count++;
            }
        }
    }

    public void UpdateColorCounts(boolean function) { //function: False if remove, True if add
        if (color == GREEN) {
            if (!function) {
                G.green_count--;
            } else {
                G.green_count++;
            }
        } else if (color == RED) {
            if (!function) {
                G.red_count--;
            } else {
                G.red_count++;
            }
        } else {
            if (!function) {
                G.yellow_count--;
            } else {
                G.yellow_count++;
            }
        }
    }

    public void Dispose() {
        UpdateCellCounts(false);
        UpdateColorCounts(false);
        super.Dispose();
    }

    public void Step() {
        double random = G.rn.Double();
        if (random < this.DEATH_PROB) {
            Dispose();
            return;
        }
        else if (random < this.FUSE_PROB + this.DEATH_PROB) {
            Fuse();
            return;
        }
        else if (random < this.BIRTH_PROB + this.FUSE_PROB + this.DEATH_PROB) {
            int nOptions = G.MapEmptyHood(G.mooreHood, Xsq(), Ysq());
            if(nOptions>0) {
                Cell child = G.NewAgentSQ(G.mooreHood[G.rn.Int(nOptions)]);
//                System.out.print(child.FUSE_PROB);
//                System.out.print(child.BIRTH_PROB);
//                System.out.print(child.DEATH_PROB + "\n");
                child.color=this.color;
                child.cell_type = this.cell_type;
                child.UpdateCellCounts(true);
                child.UpdateColorCounts(true);
                child.FUSE_PROB = G.FUSE_PROB;
                child.BIRTH_PROB = G.BIRTH_PROB;
                child.DEATH_PROB = G.DEATH_PROB; //once divides it's allowed to fuse again/reset to starting values? not sure what it was set to before I manually encoded it here
                //TODO: once genetic information is encoded, will need to allow for parasexual-style genetic mixing in the children if cell type is fused :0
            }
            return;
        }
        else {
            return;
        }
    }

    private void Fuse() {
        int nOptions = G.MapOccupiedHood(G.mooreHood, Xsq(), Ysq());
        String partner_type = null;
        int partner_color= 0;
        Cell fuse_partner = null;
        for (int i = 0; i < nOptions; i++) {
            if (partner_type != null) { //stop once we have a fusion partner
                break;
            }
            fuse_partner = G.GetAgent(G.mooreHood[G.rn.Int(nOptions)]); //get random neighbor
            String test = fuse_partner.cell_type;
            if ((test.equals("parental")) | (test.equals("resistant"))) { //only allowed to merge if the neighbor is also not a fused cell //TODO: change to allow for multiple fusions
                partner_type = test;
                partner_color = fuse_partner.color;
            }
        }
        if (partner_type != null) {
            fuse_partner.Dispose();
            if ((this.color + partner_color) == (RED + GREEN)) {
                UpdateColorCounts(false);
                this.color = Util.YELLOW; //fixed bug should only be yellow if two together are red and green to start
                UpdateColorCounts(true);
            }
            UpdateCellCounts(false);
            this.cell_type = "fused";
            UpdateCellCounts(true);
            this.FUSE_PROB = 0; //ONCE FUSED, NO MORE FUSION allowed until divide (for now //TODO change later)
            //TODO: change color only if parental is merging with resistant, but cell_type label should change regardless
            }
        }
    }


public class SimpleFusion extends AgentGrid2D<Cell> {
    // model state
    int fused_cell_count = 0;
    int parental_cell_count = 0;
    int resistant_cell_count = 0;
    int green_count = 0;
    int red_count = 0;
    int yellow_count = 0;

    //model params
    int BLACK= Util.RGB(0,0,0);
    double DEATH_PROB = 0.01;
    double BIRTH_PROB = 0.2;
    double FUSE_PROB = 0.001;
    Rand rn=new Rand();
    int[]mooreHood= Util.MooreHood(false);

    //IO params
    FileIO cellCountLogFile;
    String cellCountLogFileName;
    int logCellCountFrequency;
    int TIdx;
    //double tStep;


    //int color;
    public SimpleFusion(int x, int y) {
        super(x, y, Cell.class);
        //this.color=color;
    }
    public void Setup(double rad){
        String filename = System.getProperty("user.dir") + "/logs/FusionModel_" + java.time.LocalDateTime.now() + ".csv";
        System.out.print(filename);
        InitialiseCellLog(filename);
        int[]coords= Util.CircleHood(true,rad);
        int nCoords= MapHood(coords,xDim/2,yDim/2);
        for (int i = 0; i < nCoords ; i++) {
            //todo: define original population ratio as input
            Cell new_cell = NewAgentSQ(coords[i]);
            //assign parameters
            new_cell.FUSE_PROB = FUSE_PROB;
            new_cell.BIRTH_PROB = BIRTH_PROB;
            new_cell.DEATH_PROB = DEATH_PROB;
            if (rn.Double() <= 0.5) {
                new_cell.color=Util.RED;
                red_count++;
                new_cell.cell_type = "resistant";
                resistant_cell_count++;
            }
            else {
                new_cell.color = Util.GREEN;
                green_count++;
                new_cell.cell_type = "parental";
                parental_cell_count++;
            }
        }

    }

    public void Step() {
        for (Cell c : this) {
            c.Step();
        }
        if (TIdx % logCellCountFrequency == 0) {
            SaveCurrentCellCount(TIdx);
            //System.out.print(TIdx + "\n");
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

    public void WriteToOutput() {

    }

    public static void main(String[] args) {
        SimpleFusion t=new SimpleFusion(100,100);
        GridWindow win=new GridWindow(100,100,10);
        GifMaker gm=new GifMaker("test.gif",0,true);
        t.Setup(10);
        win.TickPause(5000);
        for (int i = 0; i < 10000; i++) {
            t.TIdx = i;
            win.TickPause(10);
            t.Step();
            t.Draw(win);
            if (i % 10 == 0) {
                gm.AddFrame(win);
            }
        }
        gm.Close();
        win.Close();
        //TODO: figure out how to store info/summary statistics about model over time with each time step in a space-aware way
    }

    // ------------------------------------------------------------------------------------------------------------
    // Manage and save output
    //
    // Credits: Following code taken and modified from github.com/eshanking/HAL_Dose_Response
    // ------------------------------------------------------------------------------------------------------------

    public void InitialiseCellLog(String cellCountLogFileName) {
        cellCountLogFile = new FileIO(cellCountLogFileName, "w");
        WriteLogFileHeader();
        this.cellCountLogFileName = cellCountLogFileName;
        this.logCellCountFrequency = 10;
    }

    private void WriteLogFileHeader() {
        // cellCountLogFile.Write("TIdx,Time,NCells_S,NCells_R,NCells,DrugConcentration,rS,rR,mS,mR,dS,dR,dD_div_S,dD_div_R,dt");

        // cellCountLogFile.Write("TIdx,Time,NCells_S,NCells_R,NCells");
        cellCountLogFile.Write("TIdx,Pop,n_fused,n_parental,n_resistant,n_yellow,n_green,n_red, fuseProb, dieProb, birthProb,");

        cellCountLogFile.Write("\n");
    }
//    public void Close() {
//        this.cellCountLogFile.Close();
//    }

    public boolean SaveCurrentCellCount(int currTimeIdx) {
        boolean successfulLog = false;
        if ((currTimeIdx % (int) (logCellCountFrequency)) == 0 && logCellCountFrequency > 0) {
            cellCountLogFile.WriteDelimit(GetModelState(),",");
            cellCountLogFile.Write("\n");
            successfulLog = true;
        }
        return successfulLog;
    }

    public double[] GetModelState() {

        return new double[] {TIdx, Pop(), fused_cell_count, parental_cell_count,
               resistant_cell_count, yellow_count, green_count, red_count, FUSE_PROB, DEATH_PROB, BIRTH_PROB};

    }

}
