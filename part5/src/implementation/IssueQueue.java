package implementation;

import baseclasses.InstructionBase;
import baseclasses.Latch;
import baseclasses.PipelineStageBase;
import cpusimulator.CpuSimulator;
import utilitytypes.*;

import java.util.ArrayList;
import java.util.EnumSet;

import static utilitytypes.EnumOpcode.FADD;
import static utilitytypes.EnumOpcode.FCMP;
import static utilitytypes.EnumOpcode.FSUB;

public class IssueQueue extends PipelineStageBase
{
    public IssueQueue(ICpuCore core)
    {
        super(core, "IssueQueue");
    }

    static final EnumSet<EnumOpcode> floatAddSubSet =
            EnumSet.of(FADD, FSUB, FCMP);

    @Override
    protected void compute()
    {
        ArrayList<Latch> WorkingInstruction = new ArrayList<>(256);
        Latch input = readInput(0).duplicate();

        InstructionBase ins = input.getInstruction();
        IGlobals globals = (GlobalData)getCore().getGlobals();


        Operand src1  = ins.getSrc1();
        Operand src2  = ins.getSrc2();
        EnumOpcode opcode = ins.getOpcode();
        Operand oper0 = ins.getOper0();
        if (ins.isNull()) return;

        boolean isdependent = false;
        boolean isduplicate = false;

        ArrayList<Latch> IssueQueueTable= ((GlobalData) globals).IssueQueueTable;
        for (int count = 0; count < IssueQueueTable.size(); count++)
        {
            InstructionBase ins_bak=IssueQueueTable.get(count).getInstruction();
            if (ins_bak.getPCAddress() == ins.getPCAddress())
            {
                isduplicate = true;
            }
        }

        if (isduplicate == false)
        {
            IssueQueueTable.add(input);
        }


        for (int count = 0; count < IssueQueueTable.size(); count++)
        {
            // InstructionBase pending = pending_ins.getInstruction();
            InstructionBase pending = IssueQueueTable.get(count).getInstruction();
            EnumOpcode pen_opcode = pending.getOpcode();
            Operand pen_src1 = pending.getSrc1();
            Operand pen_src2 = pending.getSrc2();
            Operand pen_oper0 = pending.getOper0();


            forwardingSearch(IssueQueueTable.get(count).duplicate());
            int[] srcRegs = new int[3];
            // Only want to forward to oper0 if it's a source.
            srcRegs[0] = pen_opcode.oper0IsSource() ? pen_oper0.getRegisterNumber() : -1;
            srcRegs[1] = pen_src1.getRegisterNumber();
            srcRegs[2] = pen_src2.getRegisterNumber();
            Operand[] operArray = {pen_oper0, pen_src1, pen_src2};

            for (int sn=0; sn<3; sn++) {
                int srcRegNum = srcRegs[sn];
                // Skip any operands that are not register sources
                if (srcRegNum < 0) continue;
                // Skip any that already have values
                if (operArray[sn].hasValue()) continue;

                String propname = "forward" + sn;
                if (!IssueQueueTable.get(count).hasProperty(propname)) {
                    // If any source operand is not available
                    // now or on the next cycle, then stall.
                    //Logger.out.println("Stall because no " + propname);
                    //this.setResourceWait(operArray[sn].getRegisterName());
                    // Nothing else to do.  Bail out.
                    isdependent = true;
                    break;
                    //return;
                }
            }

            if (!isdependent && IssueQueueTable.get(count).getInstruction()!=null)
            {
                WorkingInstruction.add(IssueQueueTable.get(count));
                if (CpuSimulator.printForwarding) {
                    for (int sn=0; sn<3; sn++) {
                        String propname = "forward" + sn;
                        if (IssueQueueTable.get(count).hasProperty(propname)) {
                            String operName = PipelineStageBase.operNames[sn];
                            String srcFoundIn = IssueQueueTable.get(count).getPropertyString(propname);
                            String srcRegName = operArray[sn].getRegisterName();
                            Logger.out.printf("# Posting forward %s from %s to %s next stage\n",
                                    srcRegName,
                                    srcFoundIn, operName);
                        }
                    }
                }
            }


        }


        Latch output = null;
        int output_num;
        String current_list = null;

        if (WorkingInstruction.size() != 0)
        {


            for (int j = 0; j < WorkingInstruction.size(); j++) {


                Latch curr_latch = WorkingInstruction.remove(j);
                InstructionBase current_ins = curr_latch.getInstruction();
                current_list += current_ins.toString()+" \n";
                //setActivity(act_ins.toString());



                EnumOpcode curr_opcode = current_ins.getOpcode();

                if (floatAddSubSet.contains(curr_opcode)) {

                    output_num = lookupOutput("IQToFloatAddSub");
                    output = this.newOutput(output_num);
                } else
                if (curr_opcode == EnumOpcode.DIV || opcode == EnumOpcode.MOD) {
                    output_num = lookupOutput("IQToIntDiv");
                    output = this.newOutput(output_num);
                } else
                if (curr_opcode == EnumOpcode.MUL) {
                    output_num = lookupOutput("IQToIntMul");
                    output = this.newOutput(output_num);
                }
                if (curr_opcode == EnumOpcode.FDIV) {
                    output_num = lookupOutput("IQToFloatDiv");
                    output = this.newOutput(output_num);
                } else
                if (curr_opcode == EnumOpcode.FMUL) {
                    output_num = lookupOutput("IQToFloatMul");
                    output = this.newOutput(output_num);
                } else
                    if (curr_opcode.accessesMemory()) {
                    //System.out.print("\nmemory access in IQ");
                    output_num = lookupOutput("IQToMemory");
                    output = this.newOutput(output_num);
                } else {
                    output_num = lookupOutput("IQToExecute");
                    output = this.newOutput(output_num);
                }

        }

            if(!output.canAcceptWork()) return;
            // Copy the forward# properties
            output.copyAllPropertiesFrom(input);
            // Copy the instruction
            output.setInstruction(ins);
            // Send the latch data to the next stage
            output.write();

            input.consume();
            setActivity(current_list);

            getCore().incIssued();

        }




    }



}




