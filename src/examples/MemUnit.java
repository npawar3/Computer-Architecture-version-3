/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package examples;

import baseclasses.FunctionalUnitBase;
import baseclasses.InstructionBase;
import baseclasses.Latch;
import baseclasses.ModuleBase;
import baseclasses.PipelineStageBase;
import baseclasses.PropertiesContainer;
import implementation.GlobalData;
import implementation.IntDiv;

import static utilitytypes.IProperties.MAIN_MEMORY;

import java.util.Map;
import tools.MultiStageDelayUnit;
import tools.MyALU;
import utilitytypes.IFunctionalUnit;
import utilitytypes.IGlobals;
import utilitytypes.IModule;
import utilitytypes.IPipeReg;
import utilitytypes.IPipeStage;
import utilitytypes.IProperties;
import utilitytypes.Operand;
import voidtypes.VoidProperties;

/**
 *
 * @author millerti
 */
public class MemUnit extends FunctionalUnitBase {
    public MemUnit(IModule parent, String name) {
        super(parent, name);
    }

    private static class Addr extends PipelineStageBase {
        public Addr(IModule parent) {
            // For simplicity, we just call this stage "in".
            super(parent, "in:Addr");
//            super(parent, "in:Math");  // this would be fine too
        }
        
        @Override
        public void compute(Latch input, Latch output) {
            if (input.isNull()) return;
            //doPostedForwarding(input);
            InstructionBase ins = input.getInstruction();
            setActivity(ins.toString());

            Operand oper0 = ins.getOper0();
            int oper0val = ins.getOper0().getValue();
            int source1 = ins.getSrc1().getValue();
            int source2 = ins.getSrc2().getValue();
            
            // The Memory stage no longer follows Execute.  It is an independent
            // functional unit parallel to Execute.  Therefore we must perform
            // address calculation here.
            int addr = source1 + source2;
            //GlobalData.Addr= source1 + source2;
            output.setProperty("Addr", addr);
            oper0.setIntValue(addr);
            ins.setOper0(oper0);
            //output.setResultValue(addr);
            
            output.setInstruction(ins);
        }
    }
    
    private static class LSQ extends PipelineStageBase {
        public LSQ(IModule parent) {
            // For simplicity, we just call this stage "in".
            super(parent, "LSQ");
//            super(parent, "in:Math");  // this would be fine too
        }
        
        @Override
        public void compute(Latch input, Latch output) {
            if (input.isNull()) return;
            //doPostedForwarding(input);
            InstructionBase ins = input.getInstruction();
            //output.setResultValue(input.getResultValue());
            addStatusWord("Addr="+input.getPropertyInteger("Addr"));
            output.setProperty("Addr", input.getPropertyInteger("Addr") );
            output.setInstruction(ins);
        }
    }
    private static class DCache extends PipelineStageBase {
        public DCache(IModule parent) {
            // For simplicity, we just call this stage "in".
            super(parent, "DCache");
//            super(parent, "in:Math");  // this would be fine too
        }
        
        @Override
        public void compute(Latch input, Latch output) {
            if (input.isNull()) return;
            doPostedForwarding(input);
            InstructionBase ins = input.getInstruction();
            setActivity(ins.toString());

            Operand oper0 = ins.getOper0();
            int oper0val = ins.getOper0().getValue();          
            int addr=input.getResultValue();
            addr=input.getPropertyInteger("Addr");
            int value = 0;
            IGlobals globals = (GlobalData)getCore().getGlobals();
            int[] memory = globals.getPropertyIntArray(MAIN_MEMORY);
            int output_num;
            switch (ins.getOpcode()) {
                case LOAD:
                    // Fetch the value from main memory at the address                
                    // retrieved above.
                    value = memory[addr];
                    output.setResultValue(value);
                    output.setInstruction(ins);
                    addStatusWord("Mem["+addr+"]");
                    break;
                
                case STORE:
                    // For store, the value to be stored in main memory is
                    // in oper0, which was fetched in Decode.
                    memory[addr] = oper0val;
                    addStatusWord("Mem["+addr+"]=" + ins.getOper0().getValueAsString());
                    //output.setInstruction(ins);
                    return;
                    
                default:
                    throw new RuntimeException("Non-memory instruction got into Memory stage");
            }
            output.setInstruction(ins);
        }
    }
    
    @Override
    public void createPipelineRegisters() {
        createPipeReg("AddrToLSQ");  
        createPipeReg("LsqToDCache");  
        createPipeReg("out");  
    }

    @Override
    public void createPipelineStages() {
        addPipeStage(new Addr(this));
        addPipeStage(new LSQ(this));
        addPipeStage(new DCache(this));
    }

    @Override
    public void createChildModules() {
    	//IFunctionalUnit child = new MultiStageDelayUnit(this, "LSQ",1);
        //addChildUnit(child);
        //IFunctionalUnit child1 = new MultiStageDelayUnit(this, "DCache", 1);
        //addChildUnit(child1);
       // addChildUnit(new LSQ(this, "LSQ"));
        //addChildUnit(new DCache(this, "DCache"));
    }

    @Override
    public void createConnections() {
        //addRegAlias("DCache", "out");
        connect("in:Addr", "AddrToLSQ", "LSQ");
        connect("LSQ", "LsqToDCache", "DCache");        
        connect("DCache", "out");
        //addRegAlias("LSQ.out", "out");
        //connect("in", "MathToDelay1", "LSQ");
        
    }

    @Override
    public void specifyForwardingSources() {
        addForwardingSource("out");
    }
}
