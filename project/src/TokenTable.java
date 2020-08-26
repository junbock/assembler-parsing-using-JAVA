import java.util.ArrayList;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.ScriptEngine;
/**
 * ����ڰ� �ۼ��� ���α׷� �ڵ带 �ܾ�� ���� �� ��, �ǹ̸� �м��ϰ�, ���� �ڵ�� ��ȯ�ϴ� ������ �Ѱ��ϴ� Ŭ�����̴�. <br>
 * pass2���� object code�� ��ȯ�ϴ� ������ ȥ�� �ذ��� �� ���� symbolTable�� instTable�� ������ �ʿ��ϹǷ� �̸� ��ũ��Ų��.<br>
 * section ���� �ν��Ͻ��� �ϳ��� �Ҵ�ȴ�.
 *
 */
public class TokenTable {
	public static final int MAX_OPERAND=3;
	
	/* bit ������ �������� ���� ���� */
	public static final int nFlag=32;
	public static final int iFlag=16;
	public static final int xFlag=8;
	public static final int bFlag=4;
	public static final int pFlag=2;
	public static final int eFlag=1;
	
	//��������
	public static final int aReg=0;
	public static final int xReg=1;
	public static final int lReg=2;
	public static final int bReg=3;
	public static final int sReg=4;
	public static final int tReg=5;
	public static final int fReg=6;
	public static final int pcReg=7;
	public static final int swReg=8;
	
	//���ڿ� ���İ��
	ScriptEngineManager mgr = new ScriptEngineManager();
    ScriptEngine engine = mgr.getEngineByName("JavaScript");
	
	/* Token�� �ٷ� �� �ʿ��� ���̺���� ��ũ��Ų��. */
	SymbolTable symTab;
	InstTable instTab;
	ArrayList<String> ltorg;
	//extref �迭
	ArrayList<Extref> extrefList;
	/** �� line�� �ǹ̺��� �����ϰ� �м��ϴ� ����. */
	ArrayList<Token> tokenList;
	
	//pass1�� ���� pc addr
	private static int pc;
	/**
	 * �ʱ�ȭ�ϸ鼭 symTable�� instTable�� ��ũ��Ų��.
	 * @param symTab : �ش� section�� ����Ǿ��ִ� symbol table
	 * @param instTab : instruction ���� ���ǵ� instTable
	 */
	public TokenTable(SymbolTable symTab, InstTable instTab) {
		//...
		this.symTab = symTab;
		this.instTab = instTab;
		ltorg = new ArrayList<String>();
		extrefList = new ArrayList<Extref>();
		tokenList = new ArrayList<Token>();
	}
	
	/**
	 * �Ϲ� ���ڿ��� �޾Ƽ� Token������ �и����� tokenList�� �߰��Ѵ�.
	 * @param line : �и����� ���� �Ϲ� ���ڿ�
	 */
	public int putToken(String line) {
		//�ּ�����
		if (line.charAt(0) == '.')
			return -1;
		
		int returnValue = 0; //���ϰ� 1�϶� csect end ��ū, �� �� ��ū 0 
		int format = 0;
		Token newToken = new Token(line);
		//intruction
		Instruction instruction = instTab.getInstruction(newToken.getOperator());
		
		//�� ������ �ɺ��� �߰�
		if (newToken.getLabel().length() != 0) {
			if (newToken.getOperator().equals("CSECT") == false || symTab.length() == 0) {
				if (symTab.search(newToken.getLabel()) == -1)
					symTab.putSymbol(newToken.getLabel(), pc);
				else {
					System.err.println("label error");
					System.exit(-1);
				}
			}
		}
		
		//operator�� inst�� �ִ� ���
		if (instruction == null) {
			if (newToken.getOperator().equals("END") || newToken.getOperator().equals("CSECT")
					|| newToken.getOperator().equals("LTORG")) {
				
				if (newToken.getOperator().equals("END") || newToken.getOperator().equals("CSECT"))
					returnValue = 1;
				//ltorg�� �����ų� object�� ������� ltorg �迭�� ��ū���� ����
				if (ltorg.size() > 0) {
					Token[] ltToken = new Token[ltorg.size()];
					String str;
					for (int i = 0; i < ltorg.size(); i++) {
						ltToken[i] = new Token();
						str = ltorg.get(i);
						symTab.putSymbol(str, pc);
						ltToken[i].setOperator("=");
						ltToken[i].setOperand(str.substring(1), 0);
						
						if (str.charAt(1) == 'X') {
							pc += 1;
						}
						else if (str.charAt(1) == 'C') {
							pc += str.length() - 4;
						}
						ltToken[i].setLocation(pc);
						tokenList.add(ltToken[i]);
					}
					ltorg = new ArrayList<String>();
				}
			}
			//start�� csect �� �����ϴ� ������Ʈ�� pc�� 
			if (newToken.getOperator().equals("START"))
				pc = Integer.valueOf(newToken.getOperand(0));
			
			if (newToken.getOperator().equals("CSECT"))
				pc = 0;
			
			//EQU
			if (newToken.getOperator().equals("EQU")) {
				String str = newToken.getOperand(0);
				//*�� ���� �ּҰ����� ��ü
				str = str.replaceAll("\\*", Integer.toString(pc));
				//�ɺ��� �ּҰ����� ��ü
				for (int i = 0; i < symTab.length(); i++) {
					str = str.replaceAll(symTab.getSymbol(i), symTab.getLocation(i).toString());
				}
				try {
					//���ڿ� ������ ����� ����
					symTab.modifySymbol(newToken.getLabel(), (int) engine.eval(str));
				} catch (ScriptException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
			
			//resw, resb, word, byte
			if (newToken.getOperator().equals("RESW")) {
				pc += Integer.valueOf(newToken.getOperand(0)) * 3;
			}
			
			if (newToken.getOperator().equals("RESB")) {
				pc += Integer.valueOf(newToken.getOperand(0));
			}
			
			if (newToken.getOperator().equals("WORD")) {
				pc += 3;
			}
			
			if (newToken.getOperator().equals("BYTE")) {
				if (newToken.getOperand(0).charAt(0) == 'X')
					pc += 1;
				else if (newToken.getOperand(0).charAt(0) == 'C') {
					pc += newToken.getOperand(0).length() - 3;
				}
			}
					
						
		}
		// operator�� inst�� �ִ� ���
		else {
			//format�� ���
			if (newToken.getOperator().charAt(0) == '+') {
				if (instruction.getFormat() == 34) {
					format = 4;
					newToken.setFlag(eFlag, 1);
				}
				else {
					System.err.println("format error");
					System.exit(-1);
				}
			}
			else {
				if (instruction.getFormat() == 34)
					format = 3;
				else
					format = instruction.getFormat();
					
			}
			
			//format�� 3�̰ų� 4�ΰ��
			if (format == 3 || format == 4) {
				if (newToken.getOperand(0) != null) {
					//xflag
					if (newToken.getOperand()[newToken.getOperand().length - 1].equals("X")) {
						newToken.setFlag(xFlag, 1);
						if (instruction.getNumberOfOperand() != newToken.getOperand().length - 1) {
							System.err.println("operand error " + newToken.getOperand(0));
							System.exit(-1);
						}
					}
					else if (instruction.getNumberOfOperand() != newToken.getOperand().length) {
						System.err.println("operand error");
						System.exit(-1);			
					}
					
					//iflag, nflag
					if (newToken.getOperand(0).charAt(0) == '#')
						newToken.setFlag(iFlag, 1);					
					else if (newToken.getOperand(0).charAt(0) == '@')
						newToken.setFlag(nFlag, 1);					
					else {
						newToken.setFlag(iFlag, 1);
						newToken.setFlag(nFlag, 1);
					}
					
					if (newToken.getOperand(0).charAt(0) == '=') {						
						if (ltorg.contains(newToken.getOperand(0)) == false) {
							ltorg.add(newToken.getOperand(0));
						}
					}
					
				}
				else {
					newToken.setFlag(iFlag, 1);
					newToken.setFlag(nFlag, 1);
				}
			}
			//format��ŭ pc����
			pc += format;
		}
		
		//��ū�߰�
		newToken.setLocation(pc);
		newToken.setFormat(format);
		tokenList.add(newToken);
		
		return returnValue;
		
	}
	
	/**
	 * tokenList���� index�� �ش��ϴ� Token�� �����Ѵ�.
	 * @param index
	 * @return : index��ȣ�� �ش��ϴ� �ڵ带 �м��� Token Ŭ����
	 */
	public Token getToken(int index) {
		return tokenList.get(index);
	}
	
	/**
	 * Pass2 �������� ����Ѵ�.
	 * instruction table, symbol table ���� �����Ͽ� objectcode�� �����ϰ�, �̸� �����Ѵ�.
	 * @param index
	 */
	public void makeObjectCode(int index){
		//...
		Token token = tokenList.get(index); //��ū ����
		String str = ""; //���ڿ� ����
		if (token.getFormat() == 0) {
			//������Ʈ ������ū
			if (token.getOperator().equals("START") || token.getOperator().equals("CSECT")) {
				if (index == 0) {
					Token lastToken = tokenList.get(tokenList.size() - 2);
					str = "H";
					str = str.concat(token.getLabel());
					str = String.format("%s\t%06X%06X", str, token.getLocation(), lastToken.getLocation());
				}
			}
			//������Ʈ ��������ū extrefList�� �о� modify �ڵ带 ����
			if (token.getOperator().equals("CSECT") || token.getOperator().equals("END")) {
				if (index != 0) {
					Extref extref = null;
					for (int i = 0; i < extrefList.size(); i++) {
						extref = extrefList.get(i);
						for (int j = 0; j < extref.length(); j++) {
							if (extref.isSub(j))
								str = str.concat(String.format("M%08X-%s\n", extref.getLocation(j), extref.getRef()));
							else
								str = str.concat(String.format("M%08X+%s\n", extref.getLocation(j), extref.getRef()));
						}	
					}
				}
			}
			
			//extdef �ڵ� ����
			if (token.getOperator().equals("EXTDEF")) {
				str = "";
				for (int i = 0; i < token.getOperand().length; i++) {
					str = str.concat(token.getOperand(i));
					str = str.concat(String.format("%06X", symTab.search(token.getOperand(i))));
				}
			}
			
			//extref �ڵ� ����
			if (token.getOperator().equals("EXTREF")) {
				str = "";
				for (int i = 0; i < token.getOperand().length; i++) {
					//extrefList�� �߰�
					extrefList.add(new Extref(token.getOperand(i)));
					str = str.concat(token.getOperand(i) + "\t");
				}
			}
			
			//resw, rewb
			if (token.getOperator().equals("RESW") || token.getOperator().equals("RESB")) {
				str = token.getOperator();
				if (token.getOperator().equals("RESB"))
					token.setByteSize(Integer.valueOf(token.getOperand(0)));
				else
					token.setByteSize(Integer.valueOf(token.getOperand(0)) * 3);
			}
			
			//word
			if (token.getOperator().equals("WORD")) {
				str = token.getOperand(0);
				str = str.replaceAll("\\*", Integer.toString(pc));
				//�ɺ��� �ּҰ����� ��ü
				for (int i = 0; i < symTab.length(); i++) {
					str = str.replaceAll(symTab.getSymbol(i), symTab.getLocation(i).toString());
				}
				//extref�� 0���� ��ü�ϰ� extrefList�� �ּҰ��� +- ������ �߰�
				for (int i = 0; i < extrefList.size(); i++) {
					//extrefList�� �ּҰ��� +- ������ �߰�
					for (int j = str.indexOf(extrefList.get(i).getRef()); j >= 0; j = str.indexOf(extrefList.get(i).getRef(), j+1)) {
						if (j == 0) 
							extrefList.get(i).add((token.getLocation() - 3) * 16 * 16 + 6, false);
						else if (str.charAt(j-1) == '+')
							extrefList.get(i).add((token.getLocation() - 3) * 16 * 16 + 6, false);
						else if (str.charAt(j-1) == '-')
							extrefList.get(i).add((token.getLocation() - 3) * 16 * 16 + 6, true);
					}
					//extref�� 0���� ��ü
					if (str.equals(str.replaceAll(extrefList.get(i).getRef(), "000000")) == false) {
						str = str.replaceAll(extrefList.get(i).getRef(), "000000");
						
					}
					str = str.replaceAll(extrefList.get(i).getRef(), "000000");
					
				}
				try {
					//���ڿ� ������ ���
					str = String.format("%06X", (int) engine.eval(str));
				} catch (ScriptException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				token.setByteSize(3);
				
			}
			
			//byte�̰ų� ltorg ��ū�� ���
			if (token.getOperator().equals("BYTE") || token.getOperator().equals("=")) {
				str = "";
				if (token.getOperand(0).charAt(0) == 'X') {
					str = str.concat(token.getOperand(0).substring(2, token.getOperand(0).length() - 1));
				}
				else if (token.getOperand(0).charAt(0) == 'C') {
					int n;
					for (int i = 2; i < token.getOperand(0).length() - 1; i++) {
						n = token.getOperand(0).charAt(i);
						str = str.concat(String.format("%02X", n));
					}
				}
				token.setByteSize(token.getOperand(0).length() - 3);
			}		
		}
		else {
			int code = 0;
			int addr = 0;
			int mask = 0;
			Extref extref = null;
			Instruction inst = instTab.getInstruction(token.getOperator());
			//�ڵ忡 oparator �߰�
			code |= inst.getOpcode() << (token.getFormat() - 1) * 8;
			//format 1
			if (token.getFormat() == 1) {
				str = String.format("%02X", inst.getOpcode());
			}
			//format 2
			else if (token.getFormat() == 2) {
				for (int i = 0; i < token.getOperand().length; i++) {
					code |= this.getRegister(token.getOperand(i)) << (4 - i * 4);
				}
				str = String.format("%04X", code);
			}
			//format 3, 4
			else {
				//xflag, bflag, pflag
				if (inst.getNumberOfOperand() != 0) {
					if (token.getOperand(token.getOperand().length - 1).equals("X"))
						token.setFlag(xFlag, 1);
					if (token.getFormat() == 3 && token.getOperand().length > 0) {
						if (Integer.signum(token.getLocation() - symTab.search(token.getOperand(0))) >= (1 << 9))
							token.setFlag(bFlag, 1);
						else
							token.setFlag(pFlag, 1);
					}	
				}
				
				//�ڵ忡 nixbpe �߰�
				code |= token.getNixbpe() << ((token.getFormat() -3) * 8 + 12);
				
				//operand �ִ� ���
				if (inst.getNumberOfOperand() > 0) {
					//operand�� symbol table�� ���� ���
					if ((addr = symTab.search(token.getOperand(0))) == -1) {
						//extref ��ġ
						for (int i = 0; i < extrefList.size(); i++) {
							if (extrefList.get(i).getRef().equals(token.getOperand(0))) {
								extref = extrefList.get(i);
								break;
							}
							else if (extrefList.get(i).getRef().equals(token.getOperand(0).substring(1))) {
								extref = extrefList.get(i);
								break;
							}
								
						}
						
						//operand�� extref�� ���°�� addr�� ���
						if (extref == null) {
							if (token.getOperand(0).charAt(0) == '#')
								addr = Integer.valueOf(token.getOperand(0).substring(1));
							else {
								addr = Integer.valueOf(token.getOperand(0));
								addr -= token.getLocation();
							}
						}
						//operand�� extref�� �ִ� ��� extrefList�� �ּҰ��߰��ϰ� addr�� 0
						else {
							int extrefAddr;
							extrefAddr = (token.getLocation() - (token.getFormat() - 1)) * 16 * 16;
							extrefAddr += token.getFormat() * 2 - 3;
							extref.add(extrefAddr, false);
							addr = 0;
						}
							
					}
					//operand�� symtable�� �ִ� ��� addr ���
					else {
						addr = symTab.search(token.getOperand(0));
						addr -= token.getLocation();
					}
						
				}
				//operand�� ���� ��� addr 0
				else
					addr = 0;
				
				//addr ����ũ
				for (int i = 0; i < token.getFormat() * 4; i++) {
					mask |= 1 << i;
				}
				
				addr &= mask;
				code |= addr;
					
			}
			
			//code�� ���ڿ��� ����
			if (token.getFormat() == 4) {
				str = String.format("%08X", code);
				token.setByteSize(4);
			}
			else if (token.getFormat() == 3) {
				str = String.format("%06X", code);
				token.setByteSize(3);
			}
			else if (token.getFormat() == 2) {
				str = String.format("%04X", code);
				token.setByteSize(2);
			}
			else if (token.getFormat() == 1) {
				str = String.format("%02X", code);
				token.setByteSize(1);
			}
			
		}
		//objectcode �߰�
		token.setObjectCode(str);
	}
	
	/** 
	 * index��ȣ�� �ش��ϴ� object code�� �����Ѵ�.
	 * @param index
	 * @return : object code
	 */
	public String getObjectCode(int index) {
		return tokenList.get(index).getObjectCode();
	}
	
	//�������Ͱ� ����
	public int getRegister(String str) {
		int returnVal = -1;
		if (str.equals("A"))
			returnVal = TokenTable.aReg;
		else if (str.equals("X"))
			returnVal = TokenTable.xReg;
		else if (str.equals("L"))
			returnVal = TokenTable.lReg;
		else if (str.equals("B"))
			returnVal = TokenTable.bReg;
		else if (str.equals("S"))
			returnVal = TokenTable.sReg;
		else if (str.equals("T"))
			returnVal = TokenTable.tReg;
		else if (str.equals("F"))
			returnVal = TokenTable.fReg;
		else if (str.equals("PC"))
			returnVal = TokenTable.pcReg;
		else if (str.equals("SW"))
			returnVal = TokenTable.swReg;
		
		return returnVal;
	}
	
	//tokentable ������
	public int size() {
		return this.tokenList.size();
	}
}

/**
 * �� ���κ��� ����� �ڵ带 �ܾ� ������ ������ ��  �ǹ̸� �ؼ��ϴ� ���� ���Ǵ� ������ ������ �����Ѵ�. 
 * �ǹ� �ؼ��� ������ pass2���� object code�� �����Ǿ��� ���� ����Ʈ �ڵ� ���� �����Ѵ�.
 */
class Token{
	//�ǹ� �м� �ܰ迡�� ���Ǵ� ������
	private int location;
	private String label;
	private String operator;
	private String[] operand;
	private String comment;
	private int nixbpe;
	private int format;
	
	// object code ���� �ܰ迡�� ���Ǵ� ������ 
	private String objectCode;
	private int byteSize;
	
	/**
	 * Ŭ������ �ʱ�ȭ �ϸ鼭 �ٷ� line�� �ǹ� �м��� �����Ѵ�. 
	 * @param line ��������� ����� ���α׷� �ڵ�
	 */
	public Token(String line) {
		//initialize �߰�
		parsing(line);	
	}
	
	public Token() {
		operand = new String[3];
	}
	
	/**
	 * line�� �������� �м��� �����ϴ� �Լ�. Token�� �� ������ �м��� ����� �����Ѵ�.
	 * @param line ��������� ����� ���α׷� �ڵ�.
	 */
	public void parsing(String line) {
		String[] str = line.split("\t");
		label = str[0];
		operator = str[1];
		
		if (str.length > 2 && str[2].length() > 0) { 
			operand = str[2].split(",");		
		}
		else
			operand = new String[3];
		
		if (str.length > 3)
			comment = str[3];
		
		nixbpe = 0;
	}
	
	/** 
	 * n,i,x,b,p,e flag�� �����Ѵ�. <br><br>
	 * 
	 * ��� �� : setFlag(nFlag, 1); <br>
	 *   �Ǵ�     setFlag(TokenTable.nFlag, 1);
	 * 
	 * @param flag : ���ϴ� ��Ʈ ��ġ
	 * @param value : ����ְ��� �ϴ� ��. 1�Ǵ� 0���� �����Ѵ�.
	 */
	public void setFlag(int flag, int value) {
		//...
		int n;
		
		if (value == 0) {
			n = TokenTable.nFlag + TokenTable.iFlag + TokenTable.xFlag 
					+ TokenTable.bFlag + TokenTable.pFlag + TokenTable.eFlag;
			n = n - flag;
			nixbpe &= n;
			
		}
		else {
			n = flag;
			nixbpe |= n;
		}
			
	}
	
	/**
	 * ���ϴ� flag���� ���� ���� �� �ִ�. flag�� ������ ���� ���ÿ� �������� �÷��׸� ��� �� ���� �����ϴ� <br><br>
	 * 
	 * ��� �� : getFlag(nFlag) <br>
	 *   �Ǵ�     getFlag(nFlag|iFlag)
	 * 
	 * @param flags : ���� Ȯ���ϰ��� �ϴ� ��Ʈ ��ġ
	 * @return : ��Ʈ��ġ�� �� �ִ� ��. �÷��׺��� ���� 32, 16, 8, 4, 2, 1�� ���� ������ ����.
	 */
	public int getNixbpe() {
		return nixbpe;
	}
	
	public int getLocation() {
		return location;
	}
	
	public void setLocation(int location) {
		this.location = location;
	}
	
	public String getOperator() {
		return operator;
	}
	
	public void setOperator(String str) {
		operator = str;
	}
	
	public String getOperand(int index) {
		return operand[index];
	}
	
	public String[] getOperand() {
		return operand;
	}
	
	public void setOperand(String str, int index) {
		operand[index] = new String(str);
	}
	
	public String getLabel() {
		return label;
	}
	
	public void setLabel(String str) {
		label = str;
	}
	
	public void setFormat(int format) {
		this.format = format;
	}
	
	public int getFormat() {
		return format;
	}
	
	public void setObjectCode(String str) {
		objectCode = str;
	}
	
	public String getObjectCode() {
		return objectCode;
	}
	
	public void setByteSize(int byteSize) {
		this.byteSize = byteSize;
	}
	
	public int getByteSize() {
		return byteSize;
	}
}
