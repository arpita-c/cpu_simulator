/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package implementation;


import baseclasses.CpuCore;
import tools.InstructionSequence;
import utilitytypes.IPipeStage;
import static utilitytypes.IProperties.*;
import utilitytypes.Logger;

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
    }
    
    public void loadProgram(InstructionSequence program) {
        getGlobals().loadProgram(program);
    }
    
    public void runProgram() {
        properties.setProperty("running", true);
        while (properties.getPropertyBoolean("running")) {
            Logger.out.println("## Cycle number: " + cycle_number);
            advanceClock();
        }
    }

    @Override
    public void createPipelineRegisters() {
        createPipeReg("FetchToDecode");
        createPipeReg("DecodeToExecute");
        //createPipeReg("DecodeToMemory");
        //createPipeReg("DecodeToMSFU");
        createPipeReg("ExecuteToWriteback");
        //createPipeReg("MemoryToWriteback");

        createPipeReg("DecodeToFAddSub");
        createPipeReg("DecodeToFDiv");
        createPipeReg("DecodeToIDiv");
        createPipeReg("DecodeToIMul");
        createPipeReg("DecodeToFMul");
        createPipeReg("DecodeToMemUnit");

        new FDiv(this,"FDiv").createPipelineRegisters();
        new FMul(this,"FMul").createPipelineRegisters();
        new FAddSub(this,"FAddSub").createPipelineRegisters();
        new IDiv(this,"IDiv").createPipelineRegisters();
        new IMul(this,"IMul").createPipelineRegisters();
        new MemUnit(this,"MemUnit").createPipelineRegisters();

    }

    @Override
    public void createPipelineStages() {
        addPipeStage(new AllMyStages.Fetch(this));
        addPipeStage(new AllMyStages.Decode(this));
        addPipeStage(new AllMyStages.Execute(this));
        //addPipeStage(new AllMyStages.Memory(this));
        addPipeStage(new AllMyStages.Writeback(this));

        new FDiv(this,"FDiv").createPipelineStages();
        new FMul(this,"FMul").createPipelineStages();
        new FAddSub(this,"FAddSub").createPipelineStages();
        new IDiv(this,"IDiv").createPipelineStages();
        new IMul(this,"IMul").createPipelineStages();
        new MemUnit(this,"MemUnit").createPipelineStages();

    }

    @Override
    public void createChildModules() {
        // MSFU is an example multistage functional unit.  Use this as a
        // basis for FMul, IMul, and FAddSub functional units.
        //addChildUnit(new MultiStageFunctionalUnit(this, "MSFU"));


        addChildUnit(new FMul(this, "FMul"));
        addChildUnit(new FAddSub(this, "FAddSub"));
        addChildUnit(new IMul(this, "IMul"));
        addChildUnit(new IDiv(this, "IDiv"));
        addChildUnit(new FDiv(this, "FDiv"));
        addChildUnit(new MemUnit(this,"MemUnit"));

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
        
        // Connect two stages through a pipeline register
        connect("Fetch", "FetchToDecode", "Decode");
        
        // Decode has multiple output registers, connecting to different
        // execute units.  
        // "MSFU" is an example multistage functional unit.  Those that
        // follow the convention of having a single input stage and single
        // output register can be connected simply my naming the functional
        // unit.  The input to MSFU is really called "MSFU.in".
        connect("Decode", "DecodeToExecute", "Execute");
       // connect("Decode", "DecodeToMemory", "Memory");
        //connect("Decode", "DecodeToMSFU", "MSFU");
        
        // Writeback has multiple input connections from different execute
        // units.  The output from MSFU is really called "MSFU.Delay.out",
        // which was aliased to "MSFU.out" so that it would be automatically
        // identified as an output from MSFU.
        connect("Execute","ExecuteToWriteback", "Writeback");
        //connect("Memory", "MemoryToWriteback", "Writeback");
        //connect("MSFU", "Writeback");



        connect("Decode", "DecodeToIDiv", "IDiv");
        connect("Decode", "DecodeToFDiv", "FDiv");
        connect("Decode", "DecodeToMemUnit", "MemUnit");
        connect("Decode", "DecodeToIMul", "IMul");
        connect("Decode", "DecodeToFMul", "FMul");
        connect("Decode", "DecodeToFAddSub", "FAddSub");


        //connect("IDiv", "DecodeToIDiv", "Writeback");
        //connect("FDiv", "DecodeToFDiv", "Writeback");

        connect("IDiv","Writeback");
        connect("FDiv","Writeback");
        connect("MemUnit","Writeback");
        connect("IMul","Writeback");
        connect("FMul",  "Writeback");
        connect("FAddSub", "Writeback");

    }

    @Override
    public void specifyForwardingSources() {
        addForwardingSource("ExecuteToWriteback");
//        addForwardingSource("MemoryToWriteback");
        
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
