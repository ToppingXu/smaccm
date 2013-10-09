package com.rockwellcollins.atc.agree.analysis;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.ecore.EObject;
import org.osate.aadl2.AbstractConnectionEnd;
import org.osate.aadl2.BooleanLiteral;
import org.osate.aadl2.ComponentClassifier;
import org.osate.aadl2.ComponentImplementation;
import org.osate.aadl2.ConnectedElement;
import org.osate.aadl2.Connection;
import org.osate.aadl2.ConnectionEnd;
import org.osate.aadl2.Context;
import org.osate.aadl2.DataImplementation;
import org.osate.aadl2.DataPort;
import org.osate.aadl2.DataSubcomponent;
import org.osate.aadl2.DataSubcomponentType;
import org.osate.aadl2.DataType;
import org.osate.aadl2.EnumerationLiteral;
import org.osate.aadl2.EventDataPort;
import org.osate.aadl2.Feature;
import org.osate.aadl2.FeatureGroup;
import org.osate.aadl2.FeatureGroupType;
import org.osate.aadl2.IntegerLiteral;
import org.osate.aadl2.NamedElement;
import org.osate.aadl2.NamedValue;
import org.osate.aadl2.Property;
import org.osate.aadl2.PropertyExpression;
import org.osate.aadl2.RealLiteral;
import org.osate.aadl2.StringLiteral;
import org.osate.aadl2.Subcomponent;
import org.osate.aadl2.ThreadSubcomponent;
import org.osate.aadl2.modelsupport.resources.OsateResourceUtil;
import org.osate.xtext.aadl2.properties.util.EMFIndexRetrieval;
import org.osate.xtext.aadl2.properties.util.PropertyUtils;

import jkind.lustre.BinaryExpr;
import jkind.lustre.BinaryOp;
import jkind.lustre.BoolExpr;
import jkind.lustre.Equation;
import jkind.lustre.Expr;
import jkind.lustre.IdExpr;
import jkind.lustre.IfThenElseExpr;
import jkind.lustre.IntExpr;
import jkind.lustre.NamedType;
import jkind.lustre.Node;
import jkind.lustre.NodeCallExpr;
import jkind.lustre.Program;
import jkind.lustre.RealExpr;
import jkind.lustre.Type;
import jkind.lustre.UnaryExpr;
import jkind.lustre.UnaryOp;
import jkind.lustre.VarDecl;

import com.rockwellcollins.atc.agree.agree.AgreeContract;
import com.rockwellcollins.atc.agree.agree.AgreeContractSubclause;
import com.rockwellcollins.atc.agree.agree.Arg;
import com.rockwellcollins.atc.agree.agree.AssertStatement;
import com.rockwellcollins.atc.agree.agree.AssumeStatement;
import com.rockwellcollins.atc.agree.agree.BoolLitExpr;
import com.rockwellcollins.atc.agree.agree.ConstStatement;
import com.rockwellcollins.atc.agree.agree.EqStatement;
import com.rockwellcollins.atc.agree.agree.FnCallExpr;
import com.rockwellcollins.atc.agree.agree.FnDefExpr;
import com.rockwellcollins.atc.agree.agree.GetPropertyExpr;
import com.rockwellcollins.atc.agree.agree.GuaranteeStatement;
import com.rockwellcollins.atc.agree.agree.IntLitExpr;
import com.rockwellcollins.atc.agree.agree.LemmaStatement;
import com.rockwellcollins.atc.agree.agree.LiftStatement;
import com.rockwellcollins.atc.agree.agree.NestedDotID;
import com.rockwellcollins.atc.agree.agree.NodeBodyExpr;
import com.rockwellcollins.atc.agree.agree.NodeDefExpr;
import com.rockwellcollins.atc.agree.agree.NodeEq;
import com.rockwellcollins.atc.agree.agree.NodeLemma;
import com.rockwellcollins.atc.agree.agree.NodeStmt;
import com.rockwellcollins.atc.agree.agree.ParamStatement;
import com.rockwellcollins.atc.agree.agree.PreExpr;
import com.rockwellcollins.atc.agree.agree.PrevExpr;
import com.rockwellcollins.atc.agree.agree.PropertyStatement;
import com.rockwellcollins.atc.agree.agree.RealLitExpr;
import com.rockwellcollins.atc.agree.agree.SpecStatement;
import com.rockwellcollins.atc.agree.agree.ThisExpr;
import com.rockwellcollins.atc.agree.agree.util.AgreeSwitch;

public class AgreeAnnexEmitter extends AgreeSwitch<Expr> {

    //lists of all the jkind expressions from the annex
    private List<Expr> assumpExpressions;
    private List<Equation> guarExpressions;
    private List<Expr> assertExpressions;
    private List<Equation> propExpressions;
    private List<Equation> eqExpressions;
    private List<Equation> constExpressions;
    private List<Node> nodeDefExpressions;
    private final List<Equation> connExpressions = new ArrayList<Equation>();
    private final String sysGuarTag = "__SYS_GUARANTEE_";
    
    //reference map used for hyperlinking from the console
    public final Map<String, EObject> refMap = new HashMap<>();
    
    //used for preventing name collision, and for pretty printing aadl variables
    private String jKindNameTag;
    private String aadlNameTag;
    
    //a list of the parents of this component for handling "Get_Property" queries
    private List<ComponentImplementation> modelParents;

    //used for pretty printing jkind -> aadl variables
    public final Map<String, String> varRenaming = new HashMap<>();
    
    //used for printing results
    private final AgreeLayout layout;
    private final String category;
    
    //keeps track of new variables
    private final Set<AgreeVarDecl> inputVars = new HashSet<>();
    private final Set<AgreeVarDecl> internalVars = new HashSet<>();

    //the special string used to replace the "." characters in aadl
    private final String dotChar = "__";
    
    //the current subcomponent
    private Subcomponent curComp; 
    
    //print errors and warnings here
    private final AgreeLogger log = new AgreeLogger();

    public AgreeAnnexEmitter(ComponentImplementation compImpl,
            List<ComponentImplementation> modelParents,
            Subcomponent subComp,
            AgreeLayout layout,
            String category){
        this.layout = layout;
        this.curComp = subComp;
        this.modelParents = modelParents;
        this.category = category;
        
        String jStr = "";
        String aStr = "";
        
        if(subComp != null){
            jStr = subComp.getName() + dotChar;
            aStr = subComp.getName() + ".";
        }

        //populates the connection equivalences
        if(compImpl != null){
            setVarEquivs(compImpl, jStr, aStr);
        }
        
    }
    
   // ************** CASE STATEMENTS **********************

    @Override
    public Expr caseLiftStatement(LiftStatement lift){
        NestedDotID nestId = lift.getSubcomp();
        
        ComponentImplementation compImpl;
        Subcomponent subComp = (Subcomponent)nestId.getBase();
        compImpl = subComp.getComponentImplementation();
        
        LinkedList<ComponentImplementation> subModelParents = new LinkedList<>();
        subModelParents.addAll(modelParents);
        subModelParents.push(compImpl);
        AgreeAnnexEmitter subEmitter = new AgreeAnnexEmitter(
                compImpl, subModelParents, subComp, layout, category);

        
        connExpressions.addAll(subEmitter.connExpressions);
        guarExpressions.addAll(subEmitter.guarExpressions);
        assumpExpressions.addAll(subEmitter.assumpExpressions);
        constExpressions.addAll(subEmitter.constExpressions);
        assertExpressions.addAll(subEmitter.assertExpressions);
        eqExpressions.addAll(subEmitter.eqExpressions);
        nodeDefExpressions.addAll(subEmitter.nodeDefExpressions);
        
        subEmitter.inputVars.removeAll(internalVars);
        inputVars.removeAll(subEmitter.internalVars);
        //subEmitter.internalVars.removeAll(inputVars);
        
        inputVars.addAll(subEmitter.inputVars);
        internalVars.addAll(subEmitter.internalVars);
       
        return null;
    }
    
    @Override
    public Expr caseAgreeContractSubclause(AgreeContractSubclause contract) {

        return doSwitch(contract.getContract());
    }

    @Override
    public Expr caseAgreeContract(AgreeContract contract) {

        for (SpecStatement spec : contract.getSpecs()) {
            doSwitch(spec);
        }

        return null;
    }

    @Override
    public Expr caseAssumeStatement(AssumeStatement state) {

        Expr expr = doSwitch(state.getExpr());
        assumpExpressions.add(expr);
        return expr;
    }

    @Override
    public Expr caseLemmaStatement(LemmaStatement state) {
        Expr expr = doSwitch(state.getExpr());
        String guarStr = state.getStr();
        guarStr = guarStr.replace("\"", "");
        refMap.put(guarStr, state);
        IdExpr strId = new IdExpr(guarStr);
        Equation eq = new Equation(strId, expr);
        guarExpressions.add(eq);
        return expr;
    }

    @Override
    public Expr caseGuaranteeStatement(GuaranteeStatement state) {

        Expr expr = doSwitch(state.getExpr());
        String guarStr = state.getStr();
        guarStr = guarStr.replace("\"", "");
        refMap.put(guarStr, state);
        IdExpr strId = new IdExpr(guarStr);
        Equation eq = new Equation(strId, expr);
        guarExpressions.add(eq);
        return expr;
    }

    @Override
    public Expr caseAssertStatement(AssertStatement state) {

        Expr expr = doSwitch(state.getExpr());
        assertExpressions.add(expr);

        return expr;
    }

    @Override
    public Expr casePropertyStatement(PropertyStatement state) {

        Expr expr = doSwitch(state.getExpr());

        AgreeVarDecl varDecl = new AgreeVarDecl();
        varDecl.jKindStr = jKindNameTag + state.getName();
        varDecl.aadlStr = aadlNameTag + state.getName();
        varDecl.type = "bool";

        layout.addElement(category, varDecl.aadlStr, AgreeLayout.SigType.OUTPUT);

        varRenaming.put(varDecl.jKindStr, varDecl.aadlStr);
        refMap.put(varDecl.aadlStr, state);
        internalVars.add(varDecl);

        IdExpr id = new IdExpr(varDecl.jKindStr);
        Equation eq = new Equation(id, expr);
        propExpressions.add(eq);
        return expr;
    }

    // TODO: name clashes for equations are inevitable if we don't prefix
    // with scopes - e.g. multiple function or node instances.
    // See commented out code.
    // This will require name lookup/replacement for id expressions or
    // (seemingly less good) a preprocessing phase that modifies names.

    @Override
    public Expr caseEqStatement(EqStatement state) {

        Expr expr = doSwitch(state.getExpr());

        List<IdExpr> varIds = new ArrayList<IdExpr>();

        for (Arg arg : state.getLhs()) {
            String baseName = arg.getName();
            AgreeVarDecl varDecl = new AgreeVarDecl();
            varDecl.jKindStr = jKindNameTag + baseName;

            IdExpr idExpr = new IdExpr(varDecl.jKindStr);
            varIds.add(idExpr);

            varDecl.aadlStr = aadlNameTag + baseName;
            varDecl.type = arg.getType().getString();

            layout.addElement(category, varDecl.aadlStr, AgreeLayout.SigType.OUTPUT);
            

            varRenaming.put(varDecl.jKindStr, varDecl.aadlStr);
            refMap.put(varDecl.aadlStr, state);
            internalVars.add(varDecl);
        }

        Equation eq = new Equation(varIds, expr);

        eqExpressions.add(eq);
        return null;
    }

    @Override
    public Expr caseConstStatement(ConstStatement state) {
        Expr expr = doSwitch(state.getExpr());

        AgreeVarDecl varType = new AgreeVarDecl();
        varType.jKindStr = jKindNameTag + state.getName();
        varType.aadlStr = aadlNameTag + state.getName();
        varType.type = state.getType().getString();

        layout.addElement(category, varType.aadlStr, AgreeLayout.SigType.OUTPUT);


        varRenaming.put(varType.jKindStr, varType.aadlStr);
        refMap.put(varType.aadlStr, state);
        internalVars.add(varType);

        IdExpr idExpr = new IdExpr(varType.jKindStr);
        Equation eq = new Equation(idExpr, expr);

        constExpressions.add(eq);

        return null;
    }

    @Override
    public Expr caseParamStatement(ParamStatement state) {

        assert (false);
        return null;
    }

    @Override
    public Expr caseBinaryExpr(com.rockwellcollins.atc.agree.agree.BinaryExpr expr) {

        Expr leftExpr = doSwitch(expr.getLeft());
        Expr rightExpr = doSwitch(expr.getRight());

        String op = expr.getOp();

        BinaryOp binOp = null;
        switch (op) {
        case "+":
            binOp = BinaryOp.PLUS;
            break;
        case "-":
            binOp = BinaryOp.MINUS;
            break;
        case "*":
            binOp = BinaryOp.MULTIPLY;
            break;
        case "/":
            binOp = BinaryOp.DIVIDE;
            break;
        case "div":
            binOp = BinaryOp.INT_DIVIDE;
            break;
        case "<=>":
        case "=":
            binOp = BinaryOp.EQUAL;
            break;
        case "!=":
        case "<>":
            binOp = BinaryOp.NOTEQUAL;
            break;
        case ">":
            binOp = BinaryOp.GREATER;
            break;
        case "<":
            binOp = BinaryOp.LESS;
            break;
        case ">=":
            binOp = BinaryOp.GREATEREQUAL;
            break;
        case "<=":
            binOp = BinaryOp.LESSEQUAL;
            break;
        case "or":
            binOp = BinaryOp.OR;
            break;
        case "and":
            binOp = BinaryOp.AND;
            break;
        case "xor":
            binOp = BinaryOp.XOR;
            break;
        case "=>":
            binOp = BinaryOp.IMPLIES;
            break;
        case "->":
            binOp = BinaryOp.ARROW;
            break;
        }

        assert (binOp != null);
        BinaryExpr binExpr = new BinaryExpr(leftExpr, binOp, rightExpr);

        return binExpr;
    }

    @Override
    public Expr caseBoolLitExpr(BoolLitExpr expr) {
        return new BoolExpr(expr.getVal().getValue());
    }

    @Override
    public Expr caseFnCallExpr(FnCallExpr expr) {
        NestedDotID dotId = expr.getFn();
        NamedElement namedEl = AgreeEmitterUtilities.getFinalNestId(dotId);
     
        String fnName = jKindNameTag + namedEl.getName();

        boolean found = false;
        for(Node node : nodeDefExpressions){
            if(node.id.equals(fnName)){
                found = true;
                break;
            }
        }
        
        if(!found){
            NestedDotID fn = expr.getFn();
            doSwitch(AgreeEmitterUtilities.getFinalNestId(fn));
        }
        
     
        List<Expr> argResults = new ArrayList<Expr>();

        for (com.rockwellcollins.atc.agree.agree.Expr argExpr : expr.getArgs()) {
            argResults.add(doSwitch(argExpr));
        }

        NodeCallExpr nodeCall = new NodeCallExpr(fnName, argResults);

        return nodeCall;
    }

    // TODO: ordering nodes/functions in dependency order
    @Override
    public Expr caseNodeDefExpr(NodeDefExpr expr) {
        // System.out.println("Visiting caseNodeDefExpr");

        String nodeName = jKindNameTag + expr.getName();
        
        for(Node node : nodeDefExpressions){
            if(node.id.equals(nodeName)){
                return null;
            }
        }

        List<VarDecl> inputs = AgreeEmitterUtilities.argsToVarDeclList(jKindNameTag, expr.getArgs());
        List<VarDecl> outputs = AgreeEmitterUtilities.argsToVarDeclList(jKindNameTag, expr.getRets());
        NodeBodyExpr body = expr.getNodeBody();
        List<VarDecl> internals = AgreeEmitterUtilities.argsToVarDeclList(jKindNameTag, body.getLocs());
        List<Equation> eqs = new ArrayList<Equation>();
        List<String> props = new ArrayList<String>();

        for (NodeStmt stmt : body.getStmts()) {
            if (stmt instanceof NodeLemma) {
                NodeLemma nodeLemma = (NodeLemma) stmt;
                String propName = nodeLemma.getStr();
                props.add(propName);
                IdExpr eqId = new IdExpr(propName);
                Expr eqExpr = doSwitch(nodeLemma.getExpr());
                Equation eq = new Equation(eqId, eqExpr);
                eqs.add(eq);
                VarDecl lemmaVar = new VarDecl(propName, NamedType.BOOL);
                internals.add(lemmaVar);
            } else if (stmt instanceof NodeEq) {
                eqs.add(nodeEqToEq((NodeEq) stmt));
            }
        }

        nodeDefExpressions.add(new Node(nodeName, inputs, outputs, internals, eqs, props));
        return null;
    }
    
    //helper method for the above method
    private Equation nodeEqToEq(NodeEq nodeEq) {
        Expr expr = doSwitch(nodeEq.getExpr());
        List<IdExpr> ids = new ArrayList<IdExpr>();
        for (Arg arg : nodeEq.getLhs()) {
            ids.add(new IdExpr(jKindNameTag + arg.getName()));
        }
        Equation eq = new Equation(ids, expr);
        return eq;
    }

    @Override
    public Expr caseFnDefExpr(FnDefExpr expr) {

        String nodeName = jKindNameTag + expr.getName();

        for(Node node : nodeDefExpressions){
            if(node.id.equals(nodeName)){
                return null;
            }
        }

        List<VarDecl> inputs = AgreeEmitterUtilities.argsToVarDeclList(jKindNameTag, expr.getArgs());
        Expr bodyExpr = doSwitch(expr.getExpr());

        Type outType = new NamedType(expr.getType().getString());
        VarDecl outVar = new VarDecl("_outvar", outType);
        List<VarDecl> outputs = Collections.singletonList(outVar);
        Equation eq = new Equation(new IdExpr("_outvar"), bodyExpr);
        List<Equation> eqs = Collections.singletonList(eq);

        Node node = new Node(nodeName, inputs, outputs, Collections.<VarDecl> emptyList(), eqs);

        nodeDefExpressions.add(node);

        return null;
    }

    // TODO: place node definition here.

    @Override
    public Expr caseGetPropertyExpr(GetPropertyExpr expr) {

        NamedElement propName = namedElFromId(expr.getProp());
        NamedElement compName = namedElFromId(expr.getComponent());

        assert (propName instanceof Property);
        
        Property prop = (Property) propName;

        PropertyExpression propVal = AgreeEmitterUtilities.getPropExpression(modelParents, compName, prop);

        //if (propVal == null) {
        //    assert (compName instanceof Subcomponent);
        //    Subcomponent subComp = (Subcomponent) compName;
        //    ComponentImplementation comp = subComp.getComponentImplementation();
        //    propVal = getPropExpression(comp, prop);
        //}

        if(propVal == null){
            throw new AgreeException("Could not locate property value '"+
                    prop.getFullName()+"' in component '"+compName.getName()+"'");
        }
        Expr res = null;
        if (propVal != null) {
            if (propVal instanceof StringLiteral) {
                // StringLiteral value = (StringLiteral) propVal;
                // nodeStr += value.getValue() + ")";
                throw new AgreeException("Property value for '" + prop.getFullName()
                        + "' cannot be of string type");
            } else if (propVal instanceof NamedValue) {
                // NamedValue namedVal = (NamedValue) propVal;
                // AbstractNamedValue absVal = namedVal.getNamedValue();
                // assert (absVal instanceof EnumerationLiteral);
                // EnumerationLiteral enVal = (EnumerationLiteral) absVal;
                throw new AgreeException("Property value for '" + prop.getFullName()
                        + "' cannot be of enumeration type");
            } else if (propVal instanceof BooleanLiteral) {
                BooleanLiteral value = (BooleanLiteral) propVal;
                res = new BoolExpr(value.getValue());
            } else if (propVal instanceof IntegerLiteral) {
                IntegerLiteral value = (IntegerLiteral) propVal;
                res = new IntExpr(BigInteger.valueOf((long) value.getScaledValue()));
            } else {
                assert (propVal instanceof RealLiteral);
                RealLiteral value = (RealLiteral) propVal;
                res = new RealExpr(BigDecimal.valueOf(value.getValue()));
            }
        }
        assert (res != null);

        return res;
    }
    
    //helper method for above
    private NamedElement namedElFromId(EObject obj) {
        if (obj instanceof NestedDotID) {
            return AgreeEmitterUtilities.getFinalNestId((NestedDotID) obj);
        } else if (obj instanceof com.rockwellcollins.atc.agree.agree.IdExpr) {
            return ((com.rockwellcollins.atc.agree.agree.IdExpr) obj).getId();
        } else {
            assert (obj instanceof ThisExpr);
            return curComp;
        }
    }

    @Override
    public Expr caseIdExpr(com.rockwellcollins.atc.agree.agree.IdExpr expr) {
        // just make an expression of the NamedElement
        return new IdExpr(jKindNameTag + expr.getId().getName());
    }

    @Override
    public Expr caseThisExpr(ThisExpr expr) {
        assert (false);
        return null;
        // return new NamedElementExpr(curComp);
    }

    @Override
    public Expr caseIfThenElseExpr(com.rockwellcollins.atc.agree.agree.IfThenElseExpr expr) {
        Expr condExpr = doSwitch(expr.getA());
        Expr thenExpr = doSwitch(expr.getB());
        Expr elseExpr = doSwitch(expr.getC());

        Expr result = new IfThenElseExpr(condExpr, thenExpr, elseExpr);

        return result;
    }

    @Override
    public Expr caseIntLitExpr(IntLitExpr expr) {
        return new IntExpr(BigInteger.valueOf(Integer.parseInt(expr.getVal())));
    }

    @Override
    public Expr caseNestedDotID(NestedDotID Id) {

        NestedDotID orgId = Id;
        String jKindVar = "";
        String aadlVar = "";
        while (Id.getSub() != null) {
            jKindVar += Id.getBase().getName() + dotChar;
            aadlVar += Id.getBase().getName() + ".";
            Id = Id.getSub();
        }

        NamedElement namedEl = Id.getBase();

        String baseName = namedEl.getName();
        IdExpr result = new IdExpr(jKindVar + baseName);

        // tags are appended to the variables to
        // make sure that scope is properly maintained
        // in the generated lustre
        // if (!(namedEl instanceof Arg)) {
        result = new IdExpr(jKindNameTag + jKindVar + baseName);

        // TODO: this code is for the case when there is some sort of
        // "floating" port on a component. I.e., a port that is not
        // transatively connected to a feature on the top level component
        // and is not connected on one side to another internal component
        // perhaps we should throw an error here rather than creating
        // a new random input?

        // hack for making sure all inputs have been created
        if (namedEl instanceof DataSubcomponent) {
            String tempStr = result.id;
            AgreeVarDecl tempStrType = new AgreeVarDecl();
            tempStrType.jKindStr = tempStr;

            if (!inputVars.contains(tempStrType) && !internalVars.contains(tempStrType)) {

                log.logWarning("In component '"
                        + orgId.getBase().getContainingClassifier().getFullName() + "', Port '"
                        + tempStr + "' is not connected to anything. Considering it to be"
                        + " an unconstrained primary input.");

             
                // this code just creates a new PI
                tempStrType = AgreeEmitterUtilities.dataTypeToVarType((DataSubcomponent) namedEl);
                jKindVar = jKindNameTag + jKindVar + Id.getBase().getName();
                aadlVar = aadlNameTag + aadlVar + Id.getBase().getName();

                //get rid of this. this is just a sanity check
                assert(jKindVar.equals(tempStr));
                tempStrType.jKindStr = tempStr;
                tempStrType.aadlStr = aadlVar;
                
                layout.addElement(category, aadlVar, AgreeLayout.SigType.INPUT);
             
                varRenaming.put(jKindVar, aadlVar);
                refMap.put(aadlVar, Id);
                inputVars.add(tempStrType);
            }
        }

        return result;
    }

    @Override
    public Expr casePreExpr(PreExpr expr) {
        Expr res = doSwitch(expr.getExpr());

        return new UnaryExpr(UnaryOp.PRE, res);
    }

    @Override
    public Expr casePrevExpr(PrevExpr expr) {
        Expr delayExpr = doSwitch(expr.getDelay());
        Expr initExpr = doSwitch(expr.getInit());

        Expr preExpr = new UnaryExpr(UnaryOp.PRE, delayExpr);

        Expr res = new BinaryExpr(initExpr, BinaryOp.ARROW, preExpr);

        return res;
    }

    @Override
    public Expr caseRealLitExpr(RealLitExpr expr) {
        return new RealExpr(BigDecimal.valueOf(Double.parseDouble(expr.getVal())));
    }

    @Override
    public Expr caseUnaryExpr(com.rockwellcollins.atc.agree.agree.UnaryExpr expr) {
        Expr subExpr = doSwitch(expr.getExpr());

        Expr res = null;
        switch (expr.getOp()) {
        case "-":
            res = new UnaryExpr(UnaryOp.NEGATIVE, subExpr);
            break;
        case "not":
            res = new UnaryExpr(UnaryOp.NOT, subExpr);
            break;
        default:
            assert (false);
        }

        return res;
    }
    
    // fills the connExpressions lists with expressions
    // that equate variables that are connected to one another
    private void setVarEquivs(ComponentImplementation compImpl, String initJStr, String initAStr) {

        // use for checking delay
        Property commTimingProp = EMFIndexRetrieval.getPropertyDefinitionInWorkspace(
                OsateResourceUtil.getResourceSet(), "Communication_Properties::Timing");

        for (Connection conn : compImpl.getAllConnections()) {

            AbstractConnectionEnd absConnDest = conn.getDestination();
            AbstractConnectionEnd absConnSour = conn.getSource();
            assert (absConnDest instanceof ConnectedElement);
            assert (absConnSour instanceof ConnectedElement);

            EnumerationLiteral lit = PropertyUtils.getEnumLiteral(conn, commTimingProp);
            boolean delayed = !lit.getName().equals("immediate");

            ConnectionEnd destConn = ((ConnectedElement) absConnDest).getConnectionEnd();
            ConnectionEnd sourConn = ((ConnectedElement) absConnSour).getConnectionEnd();
            Context destContext = ((ConnectedElement) absConnDest).getContext();
            Context sourContext = ((ConnectedElement) absConnSour).getContext();

            Feature port = null;
            if (destConn != null) {
                port = (Feature)destConn;
                if(port instanceof FeatureGroup){
                    FeatureGroupType featType = ((FeatureGroup)port).getAllFeatureGroupType();
                    for(DataPort dPort: featType.getOwnedDataPorts()){
                        port = dPort;
                        setVarEquiv(port.getName(), initJStr, initAStr, port, sourContext, sourConn, destContext, destConn, delayed);
                    }
                }else{
                    setVarEquiv("", initJStr, initAStr, port, sourContext, sourConn, destContext, destConn, delayed);
                }
            } else if (sourConn != null) {
                port = (Feature)sourConn;
                if(port instanceof FeatureGroup){
                    FeatureGroupType featType = ((FeatureGroup)port).getAllFeatureGroupType();
                    for(DataPort dPort: featType.getOwnedDataPorts()){
                        port = dPort;
                        setVarEquiv(port.getName(), initJStr, initAStr, port, sourContext, sourConn, destContext, destConn, delayed);
                    }
                }else{
                    setVarEquiv("", initJStr, initAStr, port, sourContext, sourConn, destContext, destConn, delayed);
                }
            }
        }
    }
    
    
    private void setVarEquiv(String prefix,
            String initJStr,
            String initAStr,
            Feature port,
            Context sourContext,
            ConnectionEnd sourConn,
            Context destContext, 
            ConnectionEnd destConn,
            boolean delayed){
        
        
        DataSubcomponentType dataSub;
        if(port instanceof DataPort){
            dataSub = ((DataPort)port).getDataFeatureClassifier();
        }else{
            dataSub = ((EventDataPort)port).getDataFeatureClassifier();
        }

        if (!(dataSub instanceof DataImplementation)) {
            return;
        }

        Set<AgreeVarDecl> tempSet = new HashSet<AgreeVarDecl>();
        getAllDataNames((DataImplementation) dataSub, tempSet);

        String sourStr;
        String destStr;
        String aadlSourStr;
        String aadlDestStr;
        if (sourContext != null) { // source is not an end point
            assert (sourContext instanceof Subcomponent || sourContext instanceof FeatureGroup);
            sourStr = sourContext.getName() + dotChar + sourConn.getName();
            aadlSourStr = sourContext.getName() + "." + sourConn.getName();
        } else {
            sourStr = sourConn.getName();
            aadlSourStr = sourConn.getName();
        }

        if (destContext != null) { // destination is not an end point
            assert (destContext instanceof Subcomponent || destContext instanceof ThreadSubcomponent);
            destStr = destContext.getName() + dotChar + destConn.getName();
            aadlDestStr = destContext.getName() + "." + destConn.getName();
        } else {
            destStr = destConn.getName();
            aadlDestStr = destConn.getName();
        }

        for (AgreeVarDecl varType : tempSet) {
            //stupid hack to handle feature groups correctly
            if(!prefix.equals("")){
                varType.jKindStr = prefix + dotChar + varType.jKindStr;
                varType.aadlStr = prefix + "." + varType.aadlStr;
            }
            String newDestStr = initJStr + destStr + dotChar + varType.jKindStr;
            String newSourStr = initJStr + sourStr + dotChar + varType.jKindStr;
            String newAADLDestStr = initAStr + aadlDestStr + "." + varType.aadlStr;
            String newAADLSourStr = initAStr + aadlSourStr + "." + varType.aadlStr;

            // make an internal var for this
            varType.jKindStr = newDestStr;
            varType.aadlStr = newAADLDestStr;

            if (destContext != null) {
                layout.addElement(destContext.getName(), varType.aadlStr,
                        AgreeLayout.SigType.OUTPUT);
            }

            refMap.put(varType.aadlStr, destConn);
            varRenaming.put(varType.jKindStr, varType.aadlStr);
            internalVars.add(varType);

            // if the source context is not null, then this is a variable
            // that was not in the top level component features. Therefore
            // a new input variable must be created
            if (sourContext != null) {
                AgreeVarDecl inputVar = new AgreeVarDecl();
                inputVar.type = varType.type;
                inputVar.jKindStr = newSourStr;
                inputVar.aadlStr = newAADLSourStr;

                layout.addElement(sourContext.getName(), inputVar.aadlStr,
                        AgreeLayout.SigType.INPUT);
                varRenaming.put(inputVar.jKindStr, inputVar.aadlStr);
                refMap.put(inputVar.aadlStr, sourConn);
                inputVars.add(inputVar);
            }

            Expr connExpr = null;
            IdExpr sourId = new IdExpr(newSourStr);

            if (sourContext != null && destContext != null && delayed) {
                // this is not an input, and the output is not a terminal
                Expr initValExpr = null;
                switch (varType.type) {
                case "bool":
                    initValExpr = new BoolExpr(true);
                    break;
                case "int":
                    initValExpr = new IntExpr(BigInteger.valueOf(0));
                    break;
                case "real":
                    initValExpr = new RealExpr(BigDecimal.valueOf(0.0));
                    break;
                }
                connExpr = new UnaryExpr(UnaryOp.PRE, sourId);
                connExpr = new BinaryExpr(initValExpr, BinaryOp.ARROW, connExpr);
            } else {
                connExpr = sourId;
            }
            IdExpr destId = new IdExpr(newDestStr);
            Equation connEq = new Equation(destId, connExpr);

            connExpressions.add(connEq);
        }

    }
    
    //helper method to above
    private void getAllDataNames(DataImplementation dataImpl, Set<AgreeVarDecl> subStrTypes) {

        for (Subcomponent sub : dataImpl.getAllSubcomponents()) {
            if (sub instanceof DataSubcomponent) {
                Set<AgreeVarDecl> newStrTypes = new HashSet<AgreeVarDecl>();
                ComponentClassifier subImpl = sub.getAllClassifier();
                if (subImpl instanceof DataImplementation) {
                    getAllDataNames((DataImplementation) subImpl, newStrTypes);
                    for (AgreeVarDecl strType : newStrTypes) {
                        AgreeVarDecl newStrType = new AgreeVarDecl();
                        newStrType.jKindStr = sub.getName() + dotChar + strType.jKindStr;
                        newStrType.aadlStr = sub.getName() + "." + strType.aadlStr;
                        newStrType.type = strType.type;
                        subStrTypes.add(newStrType);
                    }
                } else {
                    assert (subImpl instanceof DataType);
                    AgreeVarDecl newStrType = AgreeEmitterUtilities.dataTypeToVarType((DataSubcomponent) sub);
                    if (newStrType.type != null) {
                        subStrTypes.add(newStrType);
                    }
                }
            }
        }
    }
    
    public List<Node> getLustre(ComponentImplementation compImpl,
            List<AgreeAnnexEmitter> subEmitters) {
        // first print out the functions which will be
        // other nodes
        
        List<String> assumProps = new ArrayList<String>();
        List<String> consistProps = new ArrayList<String>();
        List<String> guarProps = new ArrayList<String>();

        // start constructing the top level node
        List<Equation> eqs = new ArrayList<Equation>();
        List<VarDecl> inputs = new ArrayList<VarDecl>();
        List<VarDecl> outputs = new ArrayList<VarDecl>();
        List<VarDecl> internals = new ArrayList<VarDecl>();
        List<String> properties = new ArrayList<String>();

        List<Node> nodeSet = new ArrayList<Node>();

        nodeSet.addAll(this.nodeDefExpressions);
        eqs.addAll(this.constExpressions);
        eqs.addAll(this.eqExpressions);
        eqs.addAll(this.propExpressions);
        eqs.addAll(this.connExpressions);
        
        List<AgreeVarDecl> inputVars = new ArrayList<>();
        List<AgreeVarDecl> internalVars = new ArrayList<>();
        
        inputVars.addAll(this.inputVars);
        internalVars.addAll(this.internalVars);

        for(AgreeAnnexEmitter subEmitter : subEmitters){
            nodeSet.addAll(subEmitter.nodeDefExpressions);
            eqs.addAll(subEmitter.constExpressions);
            eqs.addAll(subEmitter.eqExpressions);
            eqs.addAll(subEmitter.propExpressions);
            eqs.addAll(subEmitter.connExpressions);
            
            varRenaming.putAll(subEmitter.varRenaming);
            inputVars.addAll(subEmitter.inputVars);
            internalVars.addAll(subEmitter.internalVars);
            
        }
        
        inputVars.removeAll(internalVars);

        IdExpr totalCompHistId = new IdExpr("_TOTAL_COMP_HIST");
        IdExpr sysAssumpHistId = new IdExpr("_SYSTEM_ASSUMP_HIST");

        internals.add(new VarDecl(totalCompHistId.id, new NamedType("bool")));
        internals.add(new VarDecl(sysAssumpHistId.id, new NamedType("bool")));

        // total component history
        Expr totalCompHist = new BoolExpr(true);

        for(AgreeAnnexEmitter subEmitter : subEmitters){
            totalCompHist = new BinaryExpr(totalCompHist, BinaryOp.AND, getLustreContract(subEmitter));
        }

        eqs.add(getLustreHistory(totalCompHist, totalCompHistId));

        // system assumptions
        Expr sysAssumpHist = getLustreAssumptionsAndAssertions(this);
        eqs.add(getLustreHistory(sysAssumpHist, sysAssumpHistId));

        
        //make the closure map for proving assumptions
        Map<Subcomponent, Set<Subcomponent>> closureMap = new HashMap<>();
        for(AgreeAnnexEmitter subEmitter : subEmitters){
            Set<Subcomponent> outputClosure = new HashSet<Subcomponent>();
            outputClosure.add(subEmitter.curComp);
            getOutputClosure(compImpl.getAllConnections(), outputClosure);
            closureMap.put(subEmitter.curComp, outputClosure);
        }

        
        // get the individual history variables
        for(AgreeAnnexEmitter subEmitter : subEmitters){

            Expr higherContracts = new BoolExpr(true);

            for (Subcomponent otherComp : closureMap.get(subEmitter.curComp)) {
                higherContracts = new BinaryExpr(higherContracts, BinaryOp.AND,
                        getLustreContract(getSubcomponentEmitter(otherComp, subEmitters)));
            }

            Expr contrAssumps = getLustreAssumptions(subEmitter);

            IdExpr compId = new IdExpr("_Hist_" + subEmitter.category);
            internals.add(new VarDecl(compId.id, new NamedType("bool")));

            Expr leftSide = new UnaryExpr(UnaryOp.PRE, totalCompHist);
            leftSide = new BinaryExpr(new BoolExpr(true), BinaryOp.ARROW, leftSide);
            leftSide = new BinaryExpr(sysAssumpHist, BinaryOp.AND, leftSide);
            leftSide = new BinaryExpr(higherContracts, BinaryOp.AND, leftSide);

            Expr contrHistExpr = new BinaryExpr(leftSide, BinaryOp.IMPLIES, contrAssumps);
            Equation contrHist = new Equation(compId, contrHistExpr);
            eqs.add(contrHist);
            properties.add(compId.id);
            assumProps.add(compId.id);
            String propertyName = subEmitter.category + " Assumptions";
            varRenaming.put(compId.id, propertyName);
            //layout.addElement("Top", propertyName, AgreeLayout.SigType.OUTPUT);
            layout.addElement(category, propertyName, AgreeLayout.SigType.OUTPUT);
            
            
            //add a property that is true if the contract is a contradiction
            IdExpr contrId = new IdExpr("_CONTR_HIST_" + subEmitter.category);
            IdExpr notContrId = new IdExpr("_NULL_CONTR_HIST_" + subEmitter.category);
            Expr contExpr = getLustreContract(subEmitter);
            Equation contHist = getLustreHistory(contExpr, contrId);
            Equation notContHist = new Equation(notContrId, new UnaryExpr(UnaryOp.NOT, contrId));
            eqs.add(notContHist);
            eqs.add(contHist);
            internals.add(new VarDecl(contrId.id, new NamedType("bool")));
            internals.add(new VarDecl(notContrId.id, new NamedType("bool")));
            properties.add(notContrId.id);
            consistProps.add(notContrId.id);
            //reversePropStatus.add(true);
            String contractName = subEmitter.category + " Consistant";
            varRenaming.put(notContrId.id, contractName);
            //layout.addElement("Top", contractName, AgreeLayout.SigType.OUTPUT);
            layout.addElement(category, contractName, AgreeLayout.SigType.OUTPUT);

            
            
        }

        // create individual properties for guarantees
        int i = 0;
        for (Equation guar : guarExpressions) {
            String guarName = guar.lhs.get(0).id;
            IdExpr sysGuaranteesId = new IdExpr(sysGuarTag + i);
            internals.add(new VarDecl(sysGuaranteesId.id, new NamedType("bool")));

            Expr totalSysGuarExpr = new BinaryExpr(sysAssumpHistId, BinaryOp.AND, totalCompHistId);
            totalSysGuarExpr = new BinaryExpr(totalSysGuarExpr, BinaryOp.IMPLIES, guar.expr);

            Equation finalGuar = new Equation(sysGuaranteesId, totalSysGuarExpr);
            eqs.add(finalGuar);
            properties.add(sysGuaranteesId.id);
            guarProps.add(sysGuaranteesId.id);
            //reversePropStatus.add(false);
            varRenaming.put(sysGuaranteesId.id, guarName);
            //layout.addElement("Top", "Component Guarantee " + i++, AgreeLayout.SigType.OUTPUT);
            layout.addElement(category, "Component Guarantee " + i++, AgreeLayout.SigType.OUTPUT);


        }
        
        //check for contradiction in total component history
        IdExpr notTotalCompHistId = new IdExpr("_NOT_TOTAL_COMP_HIST");
        Equation contrEq = new Equation(notTotalCompHistId, new UnaryExpr(UnaryOp.NOT, totalCompHistId));
        internals.add(new VarDecl(notTotalCompHistId.id, new NamedType("bool")));
        eqs.add(contrEq);
        properties.add(notTotalCompHistId.id);
        consistProps.add(notTotalCompHistId.id);
        //reversePropStatus.add(true);
        varRenaming.put(notTotalCompHistId.id, "Total Contract Consistant");
        //layout.addElement("Top", "Total Contract Consistants", AgreeLayout.SigType.OUTPUT);
        layout.addElement(category, "Total Contract Consistants", AgreeLayout.SigType.OUTPUT);


        Node topNode = new Node("_MAIN", inputs, outputs, internals, eqs, properties);
        nodeSet.add(topNode);
        return nodeSet;
    }
    
    private AgreeAnnexEmitter getSubcomponentEmitter(Subcomponent sub, 
            List<AgreeAnnexEmitter> subEmitters){
        for(AgreeAnnexEmitter subEmitter : subEmitters){
            if(subEmitter.curComp == sub){
                return subEmitter;
            }
        }
        return null;
    }
    
    private Expr conjoin(List<Expr> exprs) {
        if (exprs.isEmpty()) {
            return new BoolExpr(true);
        }

        Iterator<Expr> iterator = exprs.iterator();
        Expr result = iterator.next();
        while (iterator.hasNext()) {
            result = new BinaryExpr(result, BinaryOp.AND, iterator.next());
        }
        return result;
    }

    private Expr conjoinEqs(List<Equation> eqs) {
        if (eqs.isEmpty()) {
            return new BoolExpr(true);
        }

        Iterator<Equation> iterator = eqs.iterator();
        Expr result = iterator.next().expr;
        while (iterator.hasNext()) {
            result = new BinaryExpr(result, BinaryOp.AND, iterator.next().expr);
        }
        return result;
    }

    private Expr conjoin(Expr... exprs) {
        return conjoin(Arrays.asList(exprs));
    }

    private Expr getLustreAssumptions(AgreeAnnexEmitter emitter) {
        return conjoin(emitter.assumpExpressions);
    }

    private Expr getLustreAssumptionsAndAssertions(AgreeAnnexEmitter emitter) {
        return conjoin(conjoin(emitter.assumpExpressions), conjoin(emitter.assertExpressions));
    }

    private Expr getLustreContract(AgreeAnnexEmitter emitter) {
        return conjoin(conjoin(emitter.assumpExpressions), conjoin(emitter.assertExpressions),
                conjoinEqs(emitter.guarExpressions));
    }

    private Expr getLustreGuarantee(AgreeAnnexEmitter emitter) {
        return conjoinEqs(emitter.guarExpressions);
    }
    
    private Equation getLustreHistory(Expr expr, IdExpr histId) {

        Expr preHist = new UnaryExpr(UnaryOp.PRE, histId);
        Expr histExpr = new BinaryExpr(expr, BinaryOp.AND, preHist);
        histExpr = new BinaryExpr(expr, BinaryOp.ARROW, histExpr);

        Equation histEq = new Equation(histId, histExpr);

        return histEq;

    }
    
    static public void getOutputClosure(List<Connection> conns, Set<Subcomponent> subs) {

        assert (subs.size() == 1);
        Subcomponent orig = (Subcomponent) (subs.toArray()[0]);
        int prevSize = subs.size();
        do {
            prevSize = subs.size();
            for (Connection conn : conns) {
                AbstractConnectionEnd absConnDest = conn.getDestination();
                AbstractConnectionEnd absConnSour = conn.getSource();

                assert (absConnDest instanceof ConnectedElement);
                Context destContext = ((ConnectedElement) absConnDest).getContext();
                assert (absConnSour instanceof ConnectedElement);
                Context sourContext = ((ConnectedElement) absConnSour).getContext();
                if (sourContext != null && subs.contains(sourContext)) {
                    if (destContext != null && destContext instanceof Subcomponent) {
                        //assert (destContext instanceof Subcomponent);
                        if (orig.equals(destContext)) {
                            // there is a loop
                            subs.clear();
                            break;
                        }
                        subs.add((Subcomponent) destContext);
                    }
                }
            }
        } while (subs.size() != prevSize);

    }

    

}
