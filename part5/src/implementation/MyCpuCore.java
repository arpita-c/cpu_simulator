/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package implementation;

import baseclasses.PipelineRegister;
import baseclasses.PipelineStageBase;
import baseclasses.CpuCore;
import examples.MultiStageFunctionalUnit;
import tools.InstructionSequence;
import utilitytypes.IGlobals;
import utilitytypes.IPipeReg;
import utilitytypes.IPipeStage;
import static utilitytypes.IProperties.*;
import utilitytypes.IRegFile;
import utilitytypes.Logger;
import voidtypes.VoidRegister;

/**
 * This is an example of a class that builds a specific CPU simulator out of
 * pipeline stages and pipeline registers.
 * 
 * @author 
 */
public class MyCpuCore extends CpuCore {
    static final String[] producer_props = {RESULT_VALUE};
        
    public void initProperties() {

        properties = new GlobalData();
        IRegFile rf = ((IGlobals)properties).getRegisterFile();
        int[] rat = ((IGlobals)properties).getPropertyIntArray("rat");
        for (int r=0; r<32; r++) {
            rf.changeFlags(r, IRegFile.SET_USED, 0);
            rat[r] = r;
        }
    }
    
    public void loadProgram(InstructionSequence program) {
        getGlobals().loadProgram(program);
    }

    private void freePhysRegs() {
        IGlobals globals = getGlobals();
        IRegFile regfile = globals.getRegisterFile();
        boolean printed = false;
        for (int i=0; i<256; i++) {
            if (regfile.isUsed(i)) {
                if (!regfile.isInvalid(i) && regfile.isRenamed(i)) {
                    regfile.markUsed(i, false);
                    if (!printed) {
                        Logger.out.print("# Freeing:");
                        printed = true;
                    }
                    Logger.out.print(" P" + i);
                }
            }
        }
        if (printed) Logger.out.println();
    }
    
    public void runProgram() {
        properties.setProperty("running", true);
        while (properties.getPropertyBoolean("running")) {
            Logger.out.println("## Cycle number: " + cycle_number);
            freePhysRegs();
            advanceClock();
        }
    }

    @Override
    public void createPipelineRegisters() {
        createPipeReg("FetchToDecode");
        //createPipeReg("DecodeToExecute");
        //createPipeReg("DecodeToMemory");
        /*createPipeReg("DecodeToIntDiv");
        createPipeReg("DecodeToFloatDiv");
        // To functional units
        createPipeReg("DecodeToIntMul");
        createPipeReg("DecodeToFloatAddSub");
        createPipeReg("DecodeToFloatMul");*/
        // From individual stages to writeback
        createPipeReg("IDivToWriteback");
        createPipeReg("FDivToWriteback");
        createPipeReg("ExecuteToWriteback");
        //createPipeReg("MemoryToWriteback");

        createPipeReg("DecodeToIQ");
        createPipeReg("IQToIntDiv");
        createPipeReg("IQToFloatDiv");
        // To functional units
        createPipeReg("IQToIntMul");
        createPipeReg("IQToFloatAddSub");
        createPipeReg("IQToFloatMul");
        createPipeReg("IQToExecute");
        createPipeReg("IQToMemory");
    }

    @Override
    public void createPipelineStages() {
        addPipeStage(new AllMyStages.Fetch(this));
        addPipeStage(new AllMyStages.Decode(this));
        addPipeStage(new AllMyStages.Execute(this));
      //  addPipeStage(new AllMyStages.Memory(this));
        addPipeStage(new IntDiv(this));
        addPipeStage(new FloatDiv(this));
        addPipeStage(new AllMyStages.Writeback(this));
        addPipeStage(new IssueQueue(this));
    }

    @Override
    public void createChildModules() {
        // MSFU is an example multistage functional unit.  Use this as a
        // basis for FMul, IMul, and FAddSub functional units.
        //addChildUnit(new MultiStageFunctionalUnit(this, "MSFU"));
        addChildUnit(new MemUnit(this));
        addChildUnit(new IntMul(this));
        addChildUnit(new FloatMul(this));
        addChildUnit(new FloatAddSub(this));
    }

    @Override
    public void createConnections() {
        // Connect pipeline elements by name.  Notice that 
        // Decode has multiple outputs, able to send to Memory, Execute,
        // or any other compute stages or functional units.
        // Writeback also has multiple inputs, able to receive from 
        // any of the compute units.
        // NOTE: Memory no longer connects to Execute.  It is now a fully 
        // independent functional unit, parallel to Execute.
        
        // Connect two stages through a pipelin register
        connect("Fetch", "FetchToDecode", "Decode");
        
        // Decode has multiple output registers, connecting to different
        // execute units.  
        // "MSFU" is an example multistage functional unit.  Those that
        // follow the convention of having a single input stage and single
        // output register can be connected simply my naming the functional
        // unit.  The input to MSFU is really called "MSFU.in".
        //connect("Decode", "DecodeToExecute", "Execute");
        //connect("Decode", "DecodeToMemory", "MemUnit");
        //connect("Decode", "DecodeToMSFU", "MSFU");

//        connect("Decode", "DecodeToIntDiv", "IntDiv");            // Stage
//        connect("Decode", "DecodeToFloatDiv", "FloatDiv");        // Stage
//        // To functional units
//        connect("Decode", "DecodeToIntMul", "IntMul");            // Unit
//        connect("Decode", "DecodeToFloatAddSub", "FloatAddSub");  // Unit
//        connect("Decode", "DecodeToFloatMul", "FloatMul");        // Unit
        
        // Writeback has multiple input connections from different execute
        // units.  The output from MSFU is really called "MSFU.Delay.out",
        // which was aliased to "MSFU.out" so that it would be automatically
        // identified as an output from MSFU.
        connect("Execute","ExecuteToWriteback", "Writeback");
        //connect("MemUnit", "MemoryToWriteback", "Writeback");
        //connect("MSFU", "Writeback");
        connect("IntDiv", "IDivToWriteback", "Writeback");
        connect("FloatDiv", "FDivToWriteback", "Writeback");
        // From functional units
        connect("IntMul", "Writeback");
        connect("FloatMul", "Writeback");
        connect("FloatAddSub", "Writeback");
        connect("MemUnit", "Writeback");

        connect("Decode", "DecodeToIQ", "IssueQueue");

        connect("IssueQueue", "IQToIntDiv", "IntDiv");            // Stage
        connect("IssueQueue", "IQToFloatDiv", "FloatDiv");        // Stage
        // To functional units
        connect("IssueQueue", "IQToIntMul", "IntMul");            // Unit
        connect("IssueQueue", "IQToFloatAddSub", "FloatAddSub");  // Unit
        connect("IssueQueue", "IQToFloatMul", "FloatMul");        // Unit

        connect("IssueQueue", "IQToExecute", "Execute");
        connect("IssueQueue", "IQToMemory", "MemUnit");

    }

    @Override
    public void specifyForwardingSources() {
        addForwardingSource("ExecuteToWriteback");
        //addForwardingSource("MemoryToWriteback");
        addForwardingSource("FDivToWriteback");
        addForwardingSource("IDivToWriteback");
        // MSFU.specifyForwardingSources is where this forwarding source is added
        // addForwardingSource("MSFU.out");
    }

    @Override
    public void specifyForwardingTargets() {
        // Not really used for anything yet
    }

    @Override
    public IPipeStage getFirstStage() {
        // CpuCore will sort stages into an optimal ordering.  This provides
        // the starting point.
        return getPipeStage("Fetch");
    }
    
    public MyCpuCore() {
        super(null, "core");
        initModule();
        printHierarchy();
        Logger.out.println("");
    }
}
