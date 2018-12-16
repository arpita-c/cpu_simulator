package implementation;

import baseclasses.FunctionalUnitBase;
import baseclasses.InstructionBase;
import baseclasses.Latch;
import baseclasses.PipelineStageBase;
import tools.MultiStageDelayUnit;
import utilitytypes.EnumOpcode;
import utilitytypes.IFunctionalUnit;
import utilitytypes.IModule;

public class FAddSub extends FunctionalUnitBase {

    public FAddSub(IModule parent, String name) {
        super(parent, name);
    }

    private static class MyMathUnit extends PipelineStageBase {
        public MyMathUnit(IModule parent) {
            super(parent, "in");
        }

        @Override
        public void compute(Latch input, Latch output) {
            float result_float_value;
            if (input.isNull()) return;
            doPostedForwarding(input);
            InstructionBase ins = input.getInstruction();
            boolean isfloat = ins.getSrc1().isFloat() || ins.getSrc2().isFloat();
            if(isfloat)
            {   float source1 = Float.intBitsToFloat(ins.getSrc1().getValue());
                float source2 = Float.intBitsToFloat(ins.getSrc2().getValue());
                EnumOpcode opcode = ins.getOpcode();
                if(opcode == EnumOpcode.FSUB)
                {   result_float_value = source1 - source2;
                    int result = Float.floatToRawIntBits(result_float_value);
                    output.setResultValue(result, isfloat);
                    output.setInstruction(ins);
                }
                else if(opcode == EnumOpcode.FADD)
                {   result_float_value = source1 + source2;
                    int result = Float.floatToRawIntBits(result_float_value);
                    output.setResultValue(result, isfloat);
                    output.setInstruction(ins);
                }
                else if (opcode == EnumOpcode.FCMP)
                {   result_float_value = source1 - source2;
                    int result = Float.floatToRawIntBits(result_float_value);
                    output.setResultValue(result, isfloat);
                    output.setInstruction(ins);
                }
            }
        }
    }
      @Override
    public void createPipelineRegisters() {
        createPipeReg("FAddSubToDelay");
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
        connect("in", "FAddSubToDelay", "Delay");
    }

      @Override
    public void specifyForwardingSources() {
        addForwardingSource("out");

    }
}
