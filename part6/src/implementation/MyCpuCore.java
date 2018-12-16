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
import utilitytypes.*;

import static utilitytypes.IProperties.*;

import voidtypes.VoidRegister;

/**
 * This is an example of a class that builds a specific CPU simulator out of
 * pipeline stages and pipeline registers.
 *
 * @author
 */
public class MyCpuCore extends CpuCore {
    static final String[] producer_props = {RESULT_VALUE};

    /**
     * Method that initializes the CpuCore.
     */
    @Override
    public void initProperties() {
        // Instantiate the CPU core's property container that we call "Globals".
        properties = new GlobalData();

// Set all RAT entries to -1, mapping architectural register numbers to the ARF.
// Set all ARF entries as USED and VALID

        ClockedIntArray rat = getCore().getGlobals().getPropertyClockedIntArray("register_alias_table");
        IRegFile arf = getCore().getGlobals().getPropertyRegisterFile(ARCH_REG_FILE);
        for (int i = 0; i < 32; i++)
        {
            rat.set(i, -1);
            arf.changeFlags(i, IRegFile.SET_USED, IRegFile.CLEAR_INVALID);
        }

    }

    public void loadProgram(InstructionSequence program) {
        getGlobals().loadProgram(program);
    }

    public void runProgram() {
        properties.setProperty(IProperties.CPU_RUN_STATE, IProperties.RUN_STATE_RUNNING);
        while (properties.getPropertyInteger(IProperties.CPU_RUN_STATE) != IProperties.RUN_STATE_HALTED) {
            Logger.out.println("## Cycle number: " + cycle_number);
            Logger.out.println("# State: " + getGlobals().getPropertyInteger(IProperties.CPU_RUN_STATE));
            IClocked.advanceClockAll();
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
        createPipeReg("DecodeToLSQ");
        createPipeReg("IQToIntDiv");
        createPipeReg("IQToFloatDiv");
        // To functional units
        createPipeReg("IQToIntMul");
        createPipeReg("IQToFloatAddSub");
        createPipeReg("IQToFloatMul");
        createPipeReg("IQToExecute");
        //createPipeReg("IQToMemory");
        createPipeReg("IQToBranchResUnit");
        createPipeReg("BranchResUnitToWriteback");
        //createPipeReg("LSQToMemUnit");
    }

    @Override
    public void createPipelineStages() {
        addPipeStage(new AllMyStages.Fetch(this));
        addPipeStage(new AllMyStages.Decode(this));
        addPipeStage(new AllMyStages.Execute(this));
        //  addPipeStage(new AllMyStages.Memory(this));
        addPipeStage(new IntDiv(this));
        addPipeStage(new FloatDiv(this));
        addPipeStage(new Writeback(this));
        addPipeStage(new IssueQueue(this));
        addPipeStage(new LoadStoreQueue(this));
        addPipeStage(new BranchResUnit(this));
        addPipeStage(new Retirement(this));
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
        connect("BranchResUnit", "BranchResUnitToWriteback","Writeback");
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
        //connect("LoadStoreQueue", "MemUnit");


        connect("Decode", "DecodeToIQ", "IssueQueue");
        connect("Decode", "DecodeToLSQ", "MemUnit");

        connect("IssueQueue", "IQToExecute", "Execute");
        connect("IssueQueue", "IQToIntDiv", "IntDiv");            // Stage
        connect("IssueQueue", "IQToFloatDiv", "FloatDiv");        // Stage
        connect("IssueQueue", "IQToBranchResUnit", "BranchResUnit");
        // To functional units

        connect("IssueQueue", "IQToIntMul", "IntMul");            // Unit
        connect("IssueQueue", "IQToFloatAddSub", "FloatAddSub");  // Unit
        connect("IssueQueue", "IQToFloatMul", "FloatMul");        // Unit

        //connect("LoadStoreQueue", "LSQToMemUnit", "MemUnit");



        //connect("IssueQueue", "IQToMemory", "MemUnit");

    }

    @Override
    public void specifyForwardingSources() {
        addForwardingSource("ExecuteToWriteback");
        //addForwardingSource("MemoryToWriteback");
        addForwardingSource("IDivToWriteback");
        addForwardingSource("FDivToWriteback");

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
