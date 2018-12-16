/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package implementation;

import implementation.AllMyLatches.*;
import utilitytypes.EnumComparison;
import utilitytypes.EnumOpcode;
import baseclasses.InstructionBase;
import baseclasses.PipelineRegister;
import baseclasses.PipelineStageBase;
import utilitytypes.LabelTarget;
import utilitytypes.Operand;
import voidtypes.VoidLatch;
import baseclasses.CpuCore;

/**
 * The AllMyStages class merely collects together all of the pipeline stage
 * classes into one place.  You are free to split them out into top-level
 * classes.
 *
 * Each inner class here implements the logic for a pipeline stage.
 *
 * It is recommended that the compute methods be idempotent.  This means
 * that if compute is called multiple times in a clock cycle, it should
 * compute the same output for the same input.
 *
 * How might we make updating the program counter idempotent?
 *
 * @author arpita
 */
public class AllMyStages {
    /*** Fetch Stage ***/
    static class Fetch extends PipelineStageBase<VoidLatch,FetchToDecode> {
        public Fetch(CpuCore core, PipelineRegister input, PipelineRegister output) {
            super(core, input, output);
        }

        @Override
        public String getStatus() {
            // Generate a string that helps you debug.
            return null;
        }

        @Override
        public void compute(VoidLatch input, FetchToDecode output) {
            GlobalData globals = (GlobalData)core.getGlobalResources();
            int pc = globals.program_counter;
            // Fetch the instruction
            InstructionBase ins = globals.program.getInstructionAt(pc);
            if (ins.isNull()) return;

            // Do something idempotent to compute the next program counter.

            // Don't forget branches, which MUST be resolved in the Decode
            // stage.  You will make use of global resources to commmunicate
            // between stages.

            // Your code goes here...

            output.setInstruction(ins);
        }

        @Override
        public boolean stageWaitingOnResource() {
            // Hint:  You will need to implement this for when branches
            // are being resolved.
            return false;
        }


        /**
         * This function is to advance state to the next clock cycle and
         * can be applied to any data that must be updated but which is
         * not stored in a pipeline register.
         */
        @Override
        public void advanceClock() {
            // Hint:  You will need to implement this help with waiting
            // for branch resolution and updating the program counter.
            // Don't forget to check for stall conditions, such as when
            // nextStageCanAcceptWork() returns false.
            GlobalData globals = (GlobalData)core.getGlobalResources();
            if(nextStageCanAcceptWork()) {
                globals.program_counter += 1;
            }
        }
    }


    /*** Decode Stage ***/
    static class Decode extends PipelineStageBase<FetchToDecode,DecodeToExecute> {

        public boolean isFutureStageStalled;

        public Decode(CpuCore core, PipelineRegister input, PipelineRegister output) {
            super(core, input, output);
        }

        @Override
        public boolean stageWaitingOnResource() {
            // Hint:  You will need to implement this to deal with
            // dependencies.
            return isFutureStageStalled;
        }

        public InstructionBase execBranch(InstructionBase ins, boolean isBranch)
        {
            GlobalData globals = (GlobalData)core.getGlobalResources();
            int pc = globals.program_counter;
            LabelTarget labelName = ins.getLabelTarget();
            int labelAddress = labelName.getAddress();
            boolean isInvalid = globals.register_invalid[ins.getOper0().getRegisterNumber()];

            if(isInvalid) {
                isFutureStageStalled = true;
                globals.branchStat = GlobalData.BranchCondition.TAKEN;
            } else if(globals.branchStat == GlobalData.BranchCondition.TAKEN) {
                if(isBranch) {
                    isFutureStageStalled = true;
                    globals.branchStat = GlobalData.BranchCondition.TAKEN;
                    pc = labelAddress;
                }
                globals.branchStat = GlobalData.BranchCondition.NOBRANCH;
            }
            else {
                isFutureStageStalled = false;
            }
            globals.program_counter = pc;

            return ins;
        }

        @Override
        public void compute(FetchToDecode input, DecodeToExecute output) {
            InstructionBase ins = input.getInstruction();

            // You're going to want to do something like this:

            // VVVVV LOOK AT THIS VVVVV
            ins = ins.duplicate();
            // ^^^^^ LOOK AT THIS ^^^^^

            // The above will allow you to do things like look up register
            // values for operands in the instruction and set them but avoid
            // altering the input latch if you're in a stall condition.
            // The point is that every time you enter this method, you want
            // the instruction and other contents of the input latch to be
            // in their original state, unaffected by whatever you did
            // in this method when there was a stall condition.
            // By cloning the instruction, you can alter it however you
            // want, and if this stage is stalled, the duplicate gets thrown
            // away without affecting the original.  This helps with
            // idempotency.



            // These null instruction checks are mostly just to speed up
            // the simulation.  The Void types were created so that null
            // checks can be almost completely avoided.
            if (ins.isNull()) return;

            GlobalData globals = (GlobalData)core.getGlobalResources();
            int[] regfile = globals.register_file;

            // Do what the decode stage does:
            // - Look up source operands
            // - Decode instruction
            // - Resolve branches
            boolean comparisonFlag=false;
            isFutureStageStalled = false;
            EnumOpcode opcode = ins.getOpcode();
            Operand source1 = ins.getSrc1();
            Operand source2 = ins.getSrc2();
            Operand operand0= ins.getOper0();
            boolean source1IsRegister = ins.getSrc1().isRegister();
            boolean source2IsRegister = ins.getSrc2().isRegister();
            boolean operand0IsRegister= ins.getOper0().isRegister();
            int source1RegisterNumber = 0;
            int source2RegisterNumber = 0;
            int operand0RegisterNumber = 0;
            boolean source1RegisterInvalid = false;
            boolean source2RegisterInvalid = false;
            boolean operand0RegisterInvalid = false;
            int operand0_globalregister_val = 0;
            int source1_globalregister_val = 0;
            int source2_globalregister_val = 0;
            boolean operand0IsSource=ins.getOpcode().oper0IsSource();

            if (source1IsRegister) {
                source1RegisterNumber  = ins.getSrc1().getRegisterNumber();
                source1RegisterInvalid =  globals.register_invalid[source1RegisterNumber];
                source1_globalregister_val  = globals.register_file[source1RegisterNumber];
            }
            if (source2IsRegister) {
                source2RegisterNumber  = ins.getSrc2().getRegisterNumber();
                source2RegisterInvalid =  globals.register_invalid[source2RegisterNumber];
                source2_globalregister_val  = globals.register_file[source2RegisterNumber];
            }
            if (operand0IsRegister) {
                operand0RegisterNumber = ins.getOper0().getRegisterNumber();
                operand0RegisterInvalid = globals.register_invalid[operand0RegisterNumber];
                operand0_globalregister_val = globals.register_file[operand0RegisterNumber];
            }


            if(!opcode.isBranch())
            {

                   if(source1IsRegister)
                   {
                        int forward_1_val=core.getForwardingResultValue(1);
                        int forward_2_val=core.getForwardingResultValue(2);
                        int forward_3_val=core.getForwardingResultValue(3);

                        boolean forward_result_next_cycle_1=core.isForwardingResultValidNextCycle(1);
                        boolean forward_result_next_cycle_2=core.isForwardingResultValidNextCycle(2);
                        boolean forward_result_next_cycle_3=core.isForwardingResultValidNextCycle(3);

                        boolean forward_result_valid_1=core.isForwardingResultValid(1);
                        boolean forward_result_valid_2=core.isForwardingResultValid(2);
                        boolean forward_result_valid_3=core.isForwardingResultValid(3);



                        globals.register_invalid[source1.getRegisterNumber()] = false;
                        int forwarding_register_number = input.getForwardingDestinationRegisterNumber();

                        if(forwarding_register_number==source1RegisterNumber)
                        {
                            if (forward_result_valid_1)
                                source1.setValue(forward_1_val);
                            else if (forward_result_valid_2)
                                source1.setValue(forward_2_val);
                            else if (forward_result_valid_3)
                                source1.setValue(forward_3_val);
                            else if (forward_result_next_cycle_1 || forward_result_next_cycle_2)
                                isFutureStageStalled = true;
                        }
                        else
                        {
                            source1.setValue(source1_globalregister_val);

                        }

                    }

                    if(source2IsRegister)
                    {
                        int forward_1_val=core.getForwardingResultValue(1);
                        int forward_2_val=core.getForwardingResultValue(2);
                        int forward_3_val=core.getForwardingResultValue(3);

                        boolean forward_result_next_cycle_1=core.isForwardingResultValidNextCycle(1);
                        boolean forward_result_next_cycle_2=core.isForwardingResultValidNextCycle(2);
                        boolean forward_result_next_cycle_3=core.isForwardingResultValidNextCycle(3);

                        boolean forward_result_valid_1=core.isForwardingResultValid(1);
                        boolean forward_result_valid_2=core.isForwardingResultValid(2);
                        boolean forward_result_valid_3=core.isForwardingResultValid(3);

                        globals.register_invalid[source2.getRegisterNumber()] = false;
                        int forwarding_register_number = input.getForwardingDestinationRegisterNumber();

                        if(forwarding_register_number == source2RegisterNumber)
                        {
                            if (forward_result_valid_1)
                                source2.setValue(forward_1_val);
                            else if (forward_result_valid_2)
                                source2.setValue(forward_2_val);
                            else if (forward_result_valid_3)
                                source2.setValue(forward_3_val);
                            else if (forward_result_next_cycle_1 || forward_result_next_cycle_2)
                                isFutureStageStalled = true;
                        }
                        else
                        {
                            source2.setValue(source2_globalregister_val);

                        }

                    }

                    if(operand0IsRegister && operand0IsSource)
                    {

                        int forward_1_val=core.getForwardingResultValue(1);
                        int forward_2_val=core.getForwardingResultValue(2);
                        int forward_3_val=core.getForwardingResultValue(3);

                        boolean forward_result_next_cycle_1=core.isForwardingResultValidNextCycle(1);
                        boolean forward_result_next_cycle_2=core.isForwardingResultValidNextCycle(2);
                        boolean forward_result_next_cycle_3=core.isForwardingResultValidNextCycle(3);

                        boolean forward_result_valid_1=core.isForwardingResultValid(1);
                        boolean forward_result_valid_2=core.isForwardingResultValid(2);
                        boolean forward_result_valid_3=core.isForwardingResultValid(3);

                        globals.register_invalid[operand0.getRegisterNumber()] = false;
                        int forwarding_register_number = input.getForwardingDestinationRegisterNumber();

                        if(forwarding_register_number == operand0RegisterNumber)
                        {
                            if (forward_result_valid_1)
                                operand0.setValue(forward_1_val);
                            else if (forward_result_valid_2)
                                operand0.setValue(forward_2_val);
                            else if (forward_result_valid_3)
                                operand0.setValue(forward_3_val);
                            else if (forward_result_next_cycle_1 || forward_result_next_cycle_2)
                                isFutureStageStalled = true;
                        }
                        else
                        {
                            operand0.setValue(operand0_globalregister_val);

                        }

                    }

            }
            else if(opcode.isBranch()) {
                if(ins.getOper0().isRegister()) {
                    if(globals.register_invalid[ins.getOper0().getRegisterNumber()]) {
                        isFutureStageStalled = true;
                    }
                }
                if(opcode.toString().equals("BRA")) {
                    if(ins.getOper0().isRegister()) {

                        if(ins.getComparison().toString().equals("EQ")) {
                            if(globals.register_file[ins.getOper0().getRegisterNumber()] == 0) comparisonFlag = true;
                            ins=execBranch(ins, comparisonFlag);
                        }
                        else if(ins.getComparison().toString().equals("NE")) {
                            if(globals.register_file[ins.getOper0().getRegisterNumber()] != 0) comparisonFlag = true;
                            ins=execBranch(ins, comparisonFlag);
                        }
                        else if(ins.getComparison().toString().equals("GT")) {
                            if(globals.register_file[ins.getOper0().getRegisterNumber()] > 0) comparisonFlag = true;
                            ins=execBranch(ins, comparisonFlag);
                        }
                        else if(ins.getComparison().toString().equals("LT")) {
                            if(globals.register_file[ins.getOper0().getRegisterNumber()] < 0) comparisonFlag = true;
                            ins=execBranch(ins, comparisonFlag);
                        }
                        else if(ins.getComparison().toString().equals("LE")) {
                            if(globals.register_file[ins.getOper0().getRegisterNumber()] <= 0) comparisonFlag = true;
                            ins=execBranch(ins, comparisonFlag);
                        }
                        else if(ins.getComparison().toString().equals("GE")) {
                            if(globals.register_file[ins.getOper0().getRegisterNumber()] >= 0) comparisonFlag=true;
                            ins=execBranch(ins, comparisonFlag);
                        }
                        else {
                            globals.branchStat = GlobalData.BranchCondition.NOBRANCH;
                        }
                    }

                    regfile = globals.register_file;
                }
                else if (opcode.toString().equals("JMP")) {
                    if (globals.branchStat == GlobalData.BranchCondition.TAKEN) {
                        globals.branchStat = GlobalData.BranchCondition.NOTTAKEN;
                    }
                    else {
                        isFutureStageStalled = true;
                        globals.branchStat = GlobalData.BranchCondition.TAKEN;
                        globals.program_counter = ins.getLabelTarget().getAddress();
                    }
                }
            }

            output.setInstruction(ins);
            // Set other data that's passed to the next stage.
        }
    }


    /*** Execute Stage ***/
    static class Execute extends PipelineStageBase<DecodeToExecute,ExecuteToMemory> {
        public Execute(CpuCore core, PipelineRegister input, PipelineRegister output) {
            super(core, input, output);
        }

        @Override
        public void compute(DecodeToExecute input, ExecuteToMemory output) {
            InstructionBase ins = input.getInstruction();
            if (ins.isNull()) return;

            int source1 = ins.getSrc1().getValue();
            int source2 = ins.getSrc2().getValue();
            int oper0 =   ins.getOper0().getValue();

            Operand src1 = ins.getSrc1();
            Operand src2 = ins.getSrc2();
            Operand Oper0= ins.getOper0();

            int register_val1 = 0;
            int register_val2 = 0;
            int operand_val;

            if(src1.isRegister()) {
                register_val1 = src1.getValue();
            }
            if(src2.isRegister()) {
                register_val2 = src2.getValue();
            }
            operand_val = Oper0.getValue();

            int result = MyALU.execute(ins.getOpcode(), source1, source2, oper0);

            if(src1.isRegister()) {
                src1.setValue(register_val1);
            }
            if(src2.isRegister()) {
                src2.setValue(register_val2);
            }
            if (ins.getOpcode().toString().equals("MOVC") || ins.getOpcode().toString().equals("ADD") || ins.getOpcode().toString().equals("LOAD")) {
                Oper0.setValue(result);
            }

            // Fill output with what passes to Memory stage...
            output.setInstruction(ins);
            // Set other data that's passed to the next stage.
            output.setOutputValue(result);
        }
    }


    /*** Memory Stage ***/
    static class Memory extends PipelineStageBase<ExecuteToMemory,MemoryToWriteback> {
        public Memory(CpuCore core, PipelineRegister input, PipelineRegister output) {
            super(core, input, output);
        }

        @Override
        public void compute(ExecuteToMemory input, MemoryToWriteback output) {
            InstructionBase ins = input.getInstruction();
            if (ins.isNull()) return;

            // Access memory...

            GlobalData globals = (GlobalData)core.getGlobalResources();
            int source1 = ins.getSrc1().getValue();
            int source2 = ins.getSrc2().getValue();
            int oper0 =   ins.getOper0().getValue();
            int[] mem = globals.memory;
            int[] regfile = globals.register_file;

            if (ins.getOpcode().toString().equals("LOAD"))
                output.setOutputValue(mem[source1]);
            else if (ins.getOpcode().toString().equals("STORE"))
                mem[input.getOutputValue()] = regfile[ins.getOper0().getRegisterNumber()];
            else
                output.setOutputValue(input.getOutputValue());

            globals.memory = mem;

            output.setInstruction(ins);
            // Set other data that's passed to the next stage.
        }
    }


    /*** Writeback Stage ***/
    static class Writeback extends PipelineStageBase<MemoryToWriteback,VoidLatch> {
        public Writeback(CpuCore core, PipelineRegister input, PipelineRegister output) {
            super(core, input, output);
        }

        @Override
        public void compute(MemoryToWriteback input, VoidLatch output) {
            InstructionBase ins = input.getInstruction();
            if (ins.isNull()) return;

            // Write back result to register file
            GlobalData globals = (GlobalData)core.getGlobalResources();

            if (ins.getOpcode() == EnumOpcode.OUT)
                System.out.println("@@output: " + globals.register_file[ins.getOper0().getRegisterNumber()]);

            if (ins.getOpcode().needsWriteback()) {
                globals.register_file[ins.getOper0().getRegisterNumber()] = input.getOutputValue();
                globals.register_invalid[ins.getOper0().getRegisterNumber()] = false;
            }
            if (input.getInstruction().getOpcode() == EnumOpcode.HALT) {
                // Stop the simulation
                System.exit(0);
            }
        }
    }
}
