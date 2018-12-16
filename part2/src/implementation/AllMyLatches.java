/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package implementation;

import baseclasses.InstructionBase;
import baseclasses.LatchBase;
import utilitytypes.EnumOpcode;

/**
 * Definitions of latch contents for pipeline registers.  Pipeline registers
 * create instances of these for passing data between pipeline stages.
 *
 * AllMyLatches is merely to collect all of these classes into one place.
 * It is not necessary for you to do it this way.
 *
 * You must fill in each latch type with the kind of data that passes between
 * pipeline stages.
 *
 * @author arpita
 */
public class AllMyLatches {
    public static class FetchToDecode extends LatchBase {
        // LatchBase already includes a field for the instruction.

    }

    public static class DecodeToExecute extends LatchBase {
        // LatchBase already includes a field for the instruction.
        // What else do you need here?

        @Override
        public boolean isForwardingResultValidNextCycle()
        {
            boolean val1=getInstruction().getOpcode().needsWriteback();
            boolean val2= (getInstruction().getOpcode() != EnumOpcode.LOAD);
            boolean val3= val1&&val2;
            //return ((getInstruction().getOpcode().needsWriteback()) && (getInstruction().getOpcode() != EnumOpcode.LOAD));
            return val3;
        }


    }

    public static class ExecuteToMemory extends LatchBase {
        // LatchBase already includes a field for the instruction.
        // What do you need here?
        protected int outputValue;

        public void setOutputValue(int value)
        {
            outputValue = value;
        }
        public int getOutputValue()
        {
            return outputValue;
        }

        @Override
        public int getForwardingResultValue()
        {
            if (isForwardingResultValid())
            {
                return outputValue;
            }
            else
            {
                return -1;
            }
        }

        @Override
        public boolean isForwardingResultValid()
        {
            return (getInstruction().getOpcode().needsWriteback() && (getInstruction().getOpcode() != EnumOpcode.LOAD));
        }

        @Override
        public boolean isForwardingResultValidNextCycle()
        {
            return (getInstruction().getOpcode().needsWriteback());
        }

    }

    public static class MemoryToWriteback extends LatchBase {
        // LatchBase already includes a field for the instruction.
        // What do you need here?
        protected int outputValue;

        public void setOutputValue(int value)
        {
            outputValue = value;
        }
        public int getOutputValue()
        {
            return outputValue;
        }


        @Override
        public boolean isForwardingResultValid()
        {
            boolean val=getInstruction().getOpcode().needsWriteback();
            return val;
        }

        @Override
        public int getForwardingResultValue()
        {
            if (isForwardingResultValid())
            {
                return outputValue;
            }
            else
            {
                return -1;
            }
        }

    }
}
