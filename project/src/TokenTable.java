import java.util.ArrayList;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.ScriptEngine;
/**
 * 사용자가 작성한 프로그램 코드를 단어별로 분할 한 후, 의미를 분석하고, 최종 코드로 변환하는 과정을 총괄하는 클래스이다. <br>
 * pass2에서 object code로 변환하는 과정은 혼자 해결할 수 없고 symbolTable과 instTable의 정보가 필요하므로 이를 링크시킨다.<br>
 * section 마다 인스턴스가 하나씩 할당된다.
 *
 */
public class TokenTable {
	public static final int MAX_OPERAND=3;
	
	/* bit 조작의 가독성을 위한 선언 */
	public static final int nFlag=32;
	public static final int iFlag=16;
	public static final int xFlag=8;
	public static final int bFlag=4;
	public static final int pFlag=2;
	public static final int eFlag=1;
	
	//레지스터
	public static final int aReg=0;
	public static final int xReg=1;
	public static final int lReg=2;
	public static final int bReg=3;
	public static final int sReg=4;
	public static final int tReg=5;
	public static final int fReg=6;
	public static final int pcReg=7;
	public static final int swReg=8;
	
	//문자열 수식계산
	ScriptEngineManager mgr = new ScriptEngineManager();
    ScriptEngine engine = mgr.getEngineByName("JavaScript");
	
	/* Token을 다룰 때 필요한 테이블들을 링크시킨다. */
	SymbolTable symTab;
	InstTable instTab;
	ArrayList<String> ltorg;
	//extref 배열
	ArrayList<Extref> extrefList;
	/** 각 line을 의미별로 분할하고 분석하는 공간. */
	ArrayList<Token> tokenList;
	
	//pass1에 쓰일 pc addr
	private static int pc;
	/**
	 * 초기화하면서 symTable과 instTable을 링크시킨다.
	 * @param symTab : 해당 section과 연결되어있는 symbol table
	 * @param instTab : instruction 명세가 정의된 instTable
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
	 * 일반 문자열을 받아서 Token단위로 분리시켜 tokenList에 추가한다.
	 * @param line : 분리되지 않은 일반 문자열
	 */
	public int putToken(String line) {
		//주석무시
		if (line.charAt(0) == '.')
			return -1;
		
		int returnValue = 0; //리턴값 1일때 csect end 토큰, 그 외 토큰 0 
		int format = 0;
		Token newToken = new Token(line);
		//intruction
		Instruction instruction = instTab.getInstruction(newToken.getOperator());
		
		//라벨 있으면 심볼에 추가
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
		
		//operator가 inst에 있는 경우
		if (instruction == null) {
			if (newToken.getOperator().equals("END") || newToken.getOperator().equals("CSECT")
					|| newToken.getOperator().equals("LTORG")) {
				
				if (newToken.getOperator().equals("END") || newToken.getOperator().equals("CSECT"))
					returnValue = 1;
				//ltorg를 만났거나 object가 끝난경우 ltorg 배열을 토큰으로 생성
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
			//start와 csect 로 시작하는 오브젝트의 pc값 
			if (newToken.getOperator().equals("START"))
				pc = Integer.valueOf(newToken.getOperand(0));
			
			if (newToken.getOperator().equals("CSECT"))
				pc = 0;
			
			//EQU
			if (newToken.getOperator().equals("EQU")) {
				String str = newToken.getOperand(0);
				//*을 현재 주소값으로 대체
				str = str.replaceAll("\\*", Integer.toString(pc));
				//심볼을 주소값으로 대체
				for (int i = 0; i < symTab.length(); i++) {
					str = str.replaceAll(symTab.getSymbol(i), symTab.getLocation(i).toString());
				}
				try {
					//문자열 수식을 계산해 수정
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
		// operator가 inst에 있는 경우
		else {
			//format을 계산
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
			
			//format이 3이거나 4인경우
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
			//format만큼 pc증가
			pc += format;
		}
		
		//토큰추가
		newToken.setLocation(pc);
		newToken.setFormat(format);
		tokenList.add(newToken);
		
		return returnValue;
		
	}
	
	/**
	 * tokenList에서 index에 해당하는 Token을 리턴한다.
	 * @param index
	 * @return : index번호에 해당하는 코드를 분석한 Token 클래스
	 */
	public Token getToken(int index) {
		return tokenList.get(index);
	}
	
	/**
	 * Pass2 과정에서 사용한다.
	 * instruction table, symbol table 등을 참조하여 objectcode를 생성하고, 이를 저장한다.
	 * @param index
	 */
	public void makeObjectCode(int index){
		//...
		Token token = tokenList.get(index); //토큰 버퍼
		String str = ""; //문자열 버퍼
		if (token.getFormat() == 0) {
			//오브젝트 시작토큰
			if (token.getOperator().equals("START") || token.getOperator().equals("CSECT")) {
				if (index == 0) {
					Token lastToken = tokenList.get(tokenList.size() - 2);
					str = "H";
					str = str.concat(token.getLabel());
					str = String.format("%s\t%06X%06X", str, token.getLocation(), lastToken.getLocation());
				}
			}
			//오브젝트 마지막토큰 extrefList를 읽어 modify 코드를 생성
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
			
			//extdef 코드 생성
			if (token.getOperator().equals("EXTDEF")) {
				str = "";
				for (int i = 0; i < token.getOperand().length; i++) {
					str = str.concat(token.getOperand(i));
					str = str.concat(String.format("%06X", symTab.search(token.getOperand(i))));
				}
			}
			
			//extref 코드 생성
			if (token.getOperator().equals("EXTREF")) {
				str = "";
				for (int i = 0; i < token.getOperand().length; i++) {
					//extrefList에 추가
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
				//심볼을 주소값으로 대체
				for (int i = 0; i < symTab.length(); i++) {
					str = str.replaceAll(symTab.getSymbol(i), symTab.getLocation(i).toString());
				}
				//extref를 0으로 대체하고 extrefList에 주소값과 +- 정보를 추가
				for (int i = 0; i < extrefList.size(); i++) {
					//extrefList에 주소값과 +- 정보를 추가
					for (int j = str.indexOf(extrefList.get(i).getRef()); j >= 0; j = str.indexOf(extrefList.get(i).getRef(), j+1)) {
						if (j == 0) 
							extrefList.get(i).add((token.getLocation() - 3) * 16 * 16 + 6, false);
						else if (str.charAt(j-1) == '+')
							extrefList.get(i).add((token.getLocation() - 3) * 16 * 16 + 6, false);
						else if (str.charAt(j-1) == '-')
							extrefList.get(i).add((token.getLocation() - 3) * 16 * 16 + 6, true);
					}
					//extref를 0으로 대체
					if (str.equals(str.replaceAll(extrefList.get(i).getRef(), "000000")) == false) {
						str = str.replaceAll(extrefList.get(i).getRef(), "000000");
						
					}
					str = str.replaceAll(extrefList.get(i).getRef(), "000000");
					
				}
				try {
					//문자열 수식을 계산
					str = String.format("%06X", (int) engine.eval(str));
				} catch (ScriptException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				token.setByteSize(3);
				
			}
			
			//byte이거나 ltorg 토큰인 경우
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
			//코드에 oparator 추가
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
				
				//코드에 nixbpe 추가
				code |= token.getNixbpe() << ((token.getFormat() -3) * 8 + 12);
				
				//operand 있는 경우
				if (inst.getNumberOfOperand() > 0) {
					//operand가 symbol table에 없는 경우
					if ((addr = symTab.search(token.getOperand(0))) == -1) {
						//extref 서치
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
						
						//operand가 extref에 없는경우 addr값 계산
						if (extref == null) {
							if (token.getOperand(0).charAt(0) == '#')
								addr = Integer.valueOf(token.getOperand(0).substring(1));
							else {
								addr = Integer.valueOf(token.getOperand(0));
								addr -= token.getLocation();
							}
						}
						//operand가 extref에 있는 경우 extrefList에 주소값추가하고 addr을 0
						else {
							int extrefAddr;
							extrefAddr = (token.getLocation() - (token.getFormat() - 1)) * 16 * 16;
							extrefAddr += token.getFormat() * 2 - 3;
							extref.add(extrefAddr, false);
							addr = 0;
						}
							
					}
					//operand가 symtable에 있는 경우 addr 계산
					else {
						addr = symTab.search(token.getOperand(0));
						addr -= token.getLocation();
					}
						
				}
				//operand가 없는 경우 addr 0
				else
					addr = 0;
				
				//addr 마스크
				for (int i = 0; i < token.getFormat() * 4; i++) {
					mask |= 1 << i;
				}
				
				addr &= mask;
				code |= addr;
					
			}
			
			//code를 문자열로 생성
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
		//objectcode 추가
		token.setObjectCode(str);
	}
	
	/** 
	 * index번호에 해당하는 object code를 리턴한다.
	 * @param index
	 * @return : object code
	 */
	public String getObjectCode(int index) {
		return tokenList.get(index).getObjectCode();
	}
	
	//레지스터값 리턴
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
	
	//tokentable 사이즈
	public int size() {
		return this.tokenList.size();
	}
}

/**
 * 각 라인별로 저장된 코드를 단어 단위로 분할한 후  의미를 해석하는 데에 사용되는 변수와 연산을 정의한다. 
 * 의미 해석이 끝나면 pass2에서 object code로 변형되었을 때의 바이트 코드 역시 저장한다.
 */
class Token{
	//의미 분석 단계에서 사용되는 변수들
	private int location;
	private String label;
	private String operator;
	private String[] operand;
	private String comment;
	private int nixbpe;
	private int format;
	
	// object code 생성 단계에서 사용되는 변수들 
	private String objectCode;
	private int byteSize;
	
	/**
	 * 클래스를 초기화 하면서 바로 line의 의미 분석을 수행한다. 
	 * @param line 문장단위로 저장된 프로그램 코드
	 */
	public Token(String line) {
		//initialize 추가
		parsing(line);	
	}
	
	public Token() {
		operand = new String[3];
	}
	
	/**
	 * line의 실질적인 분석을 수행하는 함수. Token의 각 변수에 분석한 결과를 저장한다.
	 * @param line 문장단위로 저장된 프로그램 코드.
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
	 * n,i,x,b,p,e flag를 설정한다. <br><br>
	 * 
	 * 사용 예 : setFlag(nFlag, 1); <br>
	 *   또는     setFlag(TokenTable.nFlag, 1);
	 * 
	 * @param flag : 원하는 비트 위치
	 * @param value : 집어넣고자 하는 값. 1또는 0으로 선언한다.
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
	 * 원하는 flag들의 값을 얻어올 수 있다. flag의 조합을 통해 동시에 여러개의 플래그를 얻는 것 역시 가능하다 <br><br>
	 * 
	 * 사용 예 : getFlag(nFlag) <br>
	 *   또는     getFlag(nFlag|iFlag)
	 * 
	 * @param flags : 값을 확인하고자 하는 비트 위치
	 * @return : 비트위치에 들어가 있는 값. 플래그별로 각각 32, 16, 8, 4, 2, 1의 값을 리턴할 것임.
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
