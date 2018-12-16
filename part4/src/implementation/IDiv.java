package implementation;

import baseclasses.FunctionalUnitBase;
import baseclasses.InstructionBase;
import baseclasses.Latch;
import baseclasses.PipelineStageBase;
import tools.MultiStageDelayUnit;
import utilitytypes.IFunctionalUnit;
import utilitytypes.IModule;

public class IDiv extends FunctionalUnitBase {
    public IDiv(IModule parent, String name) {
        super(parent, name);
    }

    private static class MyMathUnit extends PipelineStageBase {
        public static int counter_stall = 0;
        public MyMathUnit(IModule parent) {
            super(parent, "in");
        }

        @Override
        public void compute(Latch input, Latch output) {
            if (input.isNull()) return;
            doPostedForwarding(input);
            InstructionBase ins = input.getInstruction();

            if(counter_stall>15)
            {
                int source1 = ins.getSrc1().getValue();
                int source2 = ins.getSrc2().getValue();
                if(source2!=0) {
                    int result = source1 / source2;
                    output.setResultValue(result);
                    output.setInstruction(ins);
                }
                counter_stall=0;
            }
            else
            {
                setResourceWait("wait");
                counter_stall=counter_stall+1;
                input.setInstruction(ins);

            }
        }
    }

    @Override
    public void createPipelineRegisters() {
        createPipeReg("IDivToDelay");
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
        addRegAlias("IDivToDelay", "out");
        connect("in", "out");
    }

    @Override
    public void specifyForwardingSources() {
        addForwardingSource("out");
    }
}
