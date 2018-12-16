package implementation;

import baseclasses.FunctionalUnitBase;
import baseclasses.InstructionBase;
import baseclasses.Latch;
import baseclasses.PipelineStageBase;
import utilitytypes.EnumOpcode;
import utilitytypes.IModule;

public class FDiv extends FunctionalUnitBase {
    public FDiv(IModule parent, String name) {
        super(parent, name);
    }

    private static class MyMathUnit extends PipelineStageBase {
        public static int counter_stall = 0;

        public MyMathUnit(IModule parent) {
            super(parent, "in");
        }

        @Override
        public void compute(Latch input, Latch output) {
            float result_float_value=0;
            if (input.isNull()) return;
            doPostedForwarding(input);
            InstructionBase ins = input.getInstruction();
            boolean isfloat = ins.getSrc1().isFloat() || ins.getSrc2().isFloat();
            if(counter_stall>15)
            { if(isfloat)
                {
                    float source1 = Float.intBitsToFloat(ins.getSrc1().getValue());
                    float source2 = Float.intBitsToFloat(ins.getSrc2().getValue());
                    EnumOpcode opcode = ins.getOpcode();
                    if (opcode == EnumOpcode.FDIV)
                    { if (source2 != 0)
                        { result_float_value = source1 / source2;
                            int result = Float.floatToRawIntBits(result_float_value);
                            output.setResultValue(result, isfloat);
                            output.setInstruction(ins);
                        }
                    }
                    counter_stall = 0;
                }
            }
            else
                { setResourceWait("wait");
                    counter_stall=counter_stall+1;
                    input.setInstruction(ins);
                }
        }
    }

    @Override
    public void createPipelineRegisters() {
        createPipeReg("FDivToDelay");
    }

     @Override
    public void createPipelineStages() {
        addPipeStage(new MyMathUnit(this));
    }

     @Override
    public void createChildModules() {

    }

    @Override
    public void createConnections() {
        addRegAlias("FDivToDelay", "out");
        connect("in", "out");

    }

    @Override
    public void specifyForwardingSources() {
        addForwardingSource("out");
    }
}
