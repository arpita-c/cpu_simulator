/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package implementation;

import baseclasses.InstructionBase;
import baseclasses.Latch;
import baseclasses.PipelineStageBase;

import java.util.*;

import utilitytypes.*;

import static utilitytypes.EnumOpcode.FADD;
import static utilitytypes.EnumOpcode.FCMP;
import static utilitytypes.EnumOpcode.FSUB;

/**
 * Some other students had the idea to store the Latch object containing a 
 * dispatched instruction into the IQ.  This allowed them to use the pre-
 * existing doForwardingSearch() method to scan for completing inputs for
 * instructions.  I consider that to be an excellent alternative approach
 * to what I did here.
 * 
 * @author millerti
 */
public class IssueQueue extends PipelineStageBase {
    
    public IssueQueue(IModule parent) {
        super(parent, "IssueQueue");
    }
    
    
    // Data structures...
    static final EnumSet<EnumOpcode> floatAddSubSet =
            EnumSet.of(FADD, FSUB, FCMP);
    
    @Override
    public void compute() {
        // Check run state
        
        // Put non-null input into free IQ entry
        // Don't forget to consume() input.
        
        // Check for forwarding opportunities.
        // Capture desired register inputs available now.
        // Take note of those available next cycle.
        
        // Select an instruction with valid inputs (or ones that will be
        // forwarded next cycle) to be issued to each output port.
        
        // Issue instructions
        // Don't forget to write() outputs.
        
        // Set activity string for diagnostic purposes.  Lines are delimited
        // by newline ('\n').

        Latch input = readInput(0).duplicate();
        InstructionBase ins = input.getInstruction();
        IGlobals globals = getCore().getGlobals();
        EnumOpcode opcode = ins.getOpcode();


        Latch output = null;
        int output_num;
        boolean[] output_is_used = new boolean[this.numOutputRegisters()];

        if (((GlobalData) globals).IQTable.size() <= 255 && ins.isValid())
        {
            ((GlobalData) globals).IQTable.add(input);
            input.consume();
            System.out.println("Got instruction: " + ins);
        }

        for (int i = 0; i < ((GlobalData) globals).IQTable.size(); i++)
        {
            EnumOpcode act_opcode = ((GlobalData) globals).IQTable.get(i).getInstruction().getOpcode();

            if (floatAddSubSet.contains(act_opcode)) {

                output_num = lookupOutput("IQToFloatAddSub");
                output = this.newOutput(output_num);
            } else
            if (act_opcode == EnumOpcode.FDIV) {
                output_num = lookupOutput("IQToFloatDiv");
                output = this.newOutput(output_num);
            } else
            if (act_opcode == EnumOpcode.FMUL) {
                output_num = lookupOutput("IQToFloatMul");
                output = this.newOutput(output_num);
            } else
            if (act_opcode == EnumOpcode.DIV || opcode == EnumOpcode.MOD) {
                output_num = lookupOutput("IQToIntDiv");
                output = this.newOutput(output_num);
            } else
            if (act_opcode == EnumOpcode.MUL) {
                output_num = lookupOutput("IQToIntMul");
                output = this.newOutput(output_num);
            } else
            if (act_opcode.accessesMemory()) {
                output_num = lookupOutput("IQToMemory");
                output = this.newOutput(output_num);
            } else {
                output_num = lookupOutput("IQToExecute");
                output = this.newOutput(output_num);
            }

            if(!output.canAcceptWork()) continue;
            if (output_is_used[output_num]) continue;

            Latch fwdsrch = ((GlobalData) globals).IQTable.get(i);

            forwardingSearch(fwdsrch);

            boolean is_dependent = false;

            InstructionBase pending = ((GlobalData) globals).IQTable.get(i).getInstruction();
            EnumOpcode pen_opcode = pending.getOpcode();
            Operand pen_src1 = pending.getSrc1();
            Operand pen_src2 = pending.getSrc2();
            Operand pen_oper0 = pending.getOper0();

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
                if (!((GlobalData) globals).IQTable.get(i).hasProperty(propname)) {
                    // If any source operand is not available
                    // now or on the next cycle, then stall.
                    // Nothing else to do.  Bail out.
                    is_dependent = true;
                    break;
                }
            }

            if (is_dependent) continue;

            output_is_used[output_num] = true;

            output.copyAllPropertiesFrom(fwdsrch);
            // Copy the instruction
            output.setInstruction(pending);
            // Send the latch data to the next stage
            output.write();

            ((GlobalData) globals).IQTable.remove(i);
        }
    }

    
}
