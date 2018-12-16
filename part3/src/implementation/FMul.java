package implementation;

import baseclasses.FunctionalUnitBase;
import baseclasses.InstructionBase;
import baseclasses.Latch;
import baseclasses.PipelineStageBase;
import tools.MultiStageDelayUnit;
import utilitytypes.EnumOpcode;
import utilitytypes.IFunctionalUnit;
import utilitytypes.IModule;

public class FMul extends FunctionalUnitBase {

    public FMul(IModule parent, String name) {
        super(parent, name);
    }

    private static class MyMathUnit extends PipelineStageBase {
        public MyMathUnit(IModule parent) {
                 super(parent, "in");
        }

        @Override
        public void compute(Latch input, Latch output) {
            float result_float;
            if (input.isNull()) return;
            doPostedForwarding(input);
            InstructionBase ins = input.getInstruction();
            boolean isfloat = ins.getSrc1().isFloat() || ins.getSrc2().isFloat();
            if (isfloat)
            {
                float source1 = Float.intBitsToFloat(ins.getSrc1().getValue());
                float source2 = Float.intBitsToFloat(ins.getSrc2().getValue());
                int oper0 = ins.getOper0().getValue();
                EnumOpcode opcode = ins.getOpcode();
                if (opcode == EnumOpcode.FMUL)
                {
                    result_float = source1 * source2;
                    int result = Float.floatToRawIntBits(result_float);
                    output.setResultValue(result, isfloat);
                    output.setInstruction(ins);
                }
            }
        }
    }

    @Override
    public void createPipelineRegisters() {
        createPipeReg("FMulToDelay");
    }

    @Override
    public void createPipelineStages() {
        addPipeStage(new MyMathUnit(this));
    }


    @Override
    public void createChildModules() {
        IFunctionalUnit child = new MultiStageDelayUnit(this, "Delay", 5);
        addChildUnit(child);
    }

    @Override
    public void createConnections() {
        addRegAlias("Delay.out", "out");
        connect("in", "FMulToDelay", "Delay");
    }

       @Override
    public void specifyForwardingSources() {
        addForwardingSource("out");
    }


}
