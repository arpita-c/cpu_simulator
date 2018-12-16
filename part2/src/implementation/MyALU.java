/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package implementation;

import utilitytypes.EnumOpcode;

/**
 * The code that implements the ALU has been separates out into a static
 * method in its own class.  However, this is just a design choice, and you
 * are not required to do this.
 *
 * @author arpita
 */
public class MyALU {
    static int execute(EnumOpcode opcode, int input1, int input2, int oper0) {
        int result = 0;

        // Implement code here that performs appropriate computations for
        // any instruction that requires an ALU operation.  See
        // EnumOpcode.
        if (opcode.name().equals("ADD")) result=input1+input2;
        else if (opcode.name().equals("SUB")) result=input1-input2;
        else if (opcode.name().equals("AND")) result=input1&input2;
        else if (opcode.name().equals("OR")) result=input1|input2;
        else if (opcode.name().equals("XOR")) result=input1^input2;
        else if (opcode.name().equals("CMP")) result=input1-input2;
        else if (opcode.name().equals("MULS") || opcode.name().equals("MULU")) result=input1*input2;
        else if (opcode.name().equals("DIVS") || opcode.name().equals("DIVU")) result=input1/input2;
        else if (opcode.name().equals("LOAD") || opcode.name().equals("STORE")) result=input1+input2;
        else if (opcode.name().equals("MOVC")) result=input1+input2;

        return result;
    }
}
