package implementation;

import baseclasses.FunctionalUnitBase;
import baseclasses.InstructionBase;
import baseclasses.Latch;
import baseclasses.PipelineStageBase;
import utilitytypes.IGlobals;
import utilitytypes.IModule;
import utilitytypes.Operand;

import static utilitytypes.IProperties.MAIN_MEMORY;

public class MemUnit extends FunctionalUnitBase
{
    public MemUnit(IModule parent, String name)
    {
        super(parent, name);
    }

    private static class Address extends PipelineStageBase
    {
        public Address(IModule parent) {
            super(parent, "in:Addr");
        }

        @Override
        public void compute(Latch input, Latch output) {
            if (input.isNull()) return;
            doPostedForwarding(input);
            InstructionBase ins = input.getInstruction();

            int source1 = ins.getSrc1().getValue();
            int source2 = ins.getSrc2().getValue();
            int result = source1 + source2;

            output.setResultValue(result);
            output.setInstruction(ins);
        }
    }

    private static class LSQ extends PipelineStageBase {
        public LSQ(IModule parent) {
            super(parent, "LSQ");
        }

        @Override
        public void compute(Latch input, Latch output) {
            if (input.isNull()) return;

            int resultval=input.getResultValue();
            output.setResultValue(resultval);

            output.setInstruction(input.getInstruction());
            output.copyAllPropertiesFrom(input);
        }
    }

    static class DCache extends PipelineStageBase {
        public DCache(IModule parent) {
            super(parent, "DCache");
        }

        @Override
        public void compute(Latch input, Latch output) {
            //if (input.isNull()) return;
            //InstructionBase ins = input.getInstruction();
            //[code deleted]
            if (input.isNull()) return;
            doPostedForwarding(input);
            InstructionBase ins = input.getInstruction();
            setActivity(ins.toString());

            Operand oper0 = ins.getOper0();
            int oper0val = ins.getOper0().getValue();
            int source1 = ins.getSrc1().getValue();
            int source2 = ins.getSrc2().getValue();

            // The Memory stage no longer follows Execute.  It is an independent
            // functional unit parallel to Execute.  Therefore we must perform
            // address calculation here.
            int resultval=input.getResultValue();
            int addr = resultval;
            int value = 0;
            IGlobals globals = (GlobalData)getCore().getGlobals();
            int[] memory = globals.getPropertyIntArray(MAIN_MEMORY);


            switch (ins.getOpcode()) {
                case LOAD:
                //[code deleted]
                    value = memory[addr];
                    output.setResultValue(value);
                    output.setInstruction(ins);
                    addStatusWord("Mem[" + addr + "]");
                    break;

                case STORE:

                    memory[addr] = oper0val;
                    addStatusWord("Mem[" + addr + "]=" + ins.getOper0().getValueAsString());
                    return;
            }
        }
    }
    /**
     * Create all pipeline registers.  Convenience methods named createPipeReg
     * will create a pipeline register and automatically add to the map
     * of pipeline registers.
     * <p>
     * If this is for a sub-module, be sure to create clearly defined module
     * input and output pipeline registers.  Input register names are expected
     * to be "in" or start with "in:", and output register names are expected
     * to be "out" or start with "out:".
     */
    @Override
    public void createPipelineRegisters()
    {
        createPipeReg("AddrToLSQ");
        createPipeReg("LsqToDcache");
        createPipeReg("out");
    }

    /**
     * Create instances of all of your local pipeline stages.  Be sure the
     * stage has been given a name and use addPipeStage to insert it into
     * the map of pipeline stages.
     */
    @Override
    public void createPipelineStages()
    {
        addPipeStage(new Address(this));
        addPipeStage(new LSQ(this));
        addPipeStage(new DCache(this));
    }

    /**
     * Create instances of all sub-modules, which are direct children of this
     * module.  Be sure the module has a name and use addChildUnit to insert
     * it into the map of sub-modules.
     * <p>
     * Child modules must implement IFunctionalUnit and therefore export
     * input and output pipeline registers via that interface.
     */
    @Override
    public void createChildModules()
    {

    }

    /**
     * Use connect() methods to create links between pipeline stages and
     * pipeline registers.
     */
    @Override
    public void createConnections()
    {
        connect("in:Addr", "AddrToLSQ", "LSQ");
        connect("LSQ", "LsqToDcache", "DCache");
        connect("DCache", "out");
    }

    /**
     * Use addForwardingSource(pipereg_name) to specify all pipeline registers
     * that could contain result values that could be forwarded to the inputs
     * of other pipeline stages.
     */
    @Override
    public void specifyForwardingSources()
    {
        addForwardingSource("out");
    }
}
