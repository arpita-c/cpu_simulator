package implementation;

import baseclasses.FunctionalUnitBase;
import baseclasses.InstructionBase;
import baseclasses.Latch;
import baseclasses.PipelineStageBase;
import tools.MultiStageDelayUnit;
import utilitytypes.IFunctionalUnit;
import utilitytypes.IModule;

public class IMul extends FunctionalUnitBase {
    public IMul(IModule parent, String name) {
        super(parent, name);
    }

    private static class MyMathUnit extends PipelineStageBase {
        public MyMathUnit(IModule parent) {
            super(parent, "in");
        }

        @Override
        public void compute(Latch input, Latch output) {
            if (input.isNull()) return;
            doPostedForwarding(input);
            InstructionBase ins = input.getInstruction();
            int source1 = ins.getSrc1().getValue();
            int source2 = ins.getSrc2().getValue();
            int result = source1 * source2;
            output.setResultValue(result);
            output.setInstruction(ins);
        }
    }


    @Override
    public void createPipelineRegisters() {
        createPipeReg("IMulToDelay");
    }

    @Override
    public void createPipelineStages() {
        addPipeStage(new MyMathUnit(this));
    }

    @Override
    public void createChildModules() {
        IFunctionalUnit child = new MultiStageDelayUnit(this, "Delay", 3);
        addChildUnit(child);
    }

    @Override
    public void createConnections() {
        addRegAlias("Delay.out", "out");
        connect("in", "IMulToDelay", "Delay");
    }

    @Override
    public void specifyForwardingSources() {
        addForwardingSource("out");
    }
}
