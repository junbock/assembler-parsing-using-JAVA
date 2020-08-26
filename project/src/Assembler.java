import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


/**
 * Assembler : 
 * 이 프로그램은 SIC/XE 머신을 위한 Assembler 프로그램의 메인 루틴이다.
 * 프로그램의 수행 작업은 다음과 같다. <br>
 * 1) 처음 시작하면 Instruction 명세를 읽어들여서 assembler를 세팅한다. <br>
 * 2) 사용자가 작성한 input 파일을 읽어들인 후 저장한다. <br>
 * 3) input 파일의 문장들을 단어별로 분할하고 의미를 파악해서 정리한다. (pass1) <br>
 * 4) 분석된 내용을 바탕으로 컴퓨터가 사용할 수 있는 object code를 생성한다. (pass2) <br>
 * 
 * <br><br>
 * 작성중의 유의사항 : <br>
 *  1) 새로운 클래스, 새로운 변수, 새로운 함수 선언은 얼마든지 허용됨. 단, 기존의 변수와 함수들을 삭제하거나 완전히 대체하는 것은 안된다.<br>
 *  2) 마찬가지로 작성된 코드를 삭제하지 않으면 필요에 따라 예외처리, 인터페이스 또는 상속 사용 또한 허용됨.<br>
 *  3) 모든 void 타입의 리턴값은 유저의 필요에 따라 다른 리턴 타입으로 변경 가능.<br>
 *  4) 파일, 또는 콘솔창에 한글을 출력시키지 말 것. (채점상의 이유. 주석에 포함된 한글은 상관 없음)<br>
 * 
 * <br><br>
 *  + 제공하는 프로그램 구조의 개선방법을 제안하고 싶은 분들은 보고서의 결론 뒷부분에 첨부 바랍니다. 내용에 따라 가산점이 있을 수 있습니다.
 */
public class Assembler {
	/** instruction 명세를 저장한 공간 */
	InstTable instTable;
	/** 읽어들인 input 파일의 내용을 한 줄 씩 저장하는 공간. */
	ArrayList<String> lineList;
	/** 프로그램의 section별로 symbol table을 저장하는 공간*/
	ArrayList<SymbolTable> symtabList;
	/** 프로그램의 section별로 프로그램을 저장하는 공간*/
	ArrayList<TokenTable> TokenList;
	/** 
	 * Token, 또는 지시어에 따라 만들어진 오브젝트 코드들을 출력 형태로 저장하는 공간. <br>
	 * 필요한 경우 String 대신 별도의 클래스를 선언하여 ArrayList를 교체해도 무방함.
	 */
	ArrayList<String> codeList;
	
	/**
	 * 클래스 초기화. instruction Table을 초기화와 동시에 세팅한다.
	 * 
	 * @param instFile : instruction 명세를 작성한 파일 이름. 
	 */
	public Assembler(String instFile) {
		instTable = new InstTable(instFile);
		lineList = new ArrayList<String>();
		symtabList = new ArrayList<SymbolTable>();
		TokenList = new ArrayList<TokenTable>();
		codeList = new ArrayList<String>();
	}

	/** 
	 * 어셐블러의 메인 루틴
	 */
	public static void main(String[] args) {
		Assembler assembler = new Assembler("inst.data");
		assembler.loadInputFile("input.txt");
		
		assembler.pass1();
		assembler.printSymbolTable("symtab_20142385");
		
		assembler.pass2();
		assembler.printObjectCode("output_20142385");
		
	}

	/**
	 * 작성된 codeList를 출력형태에 맞게 출력한다.<br>
	 * @param fileName : 저장되는 파일 이름
	 */
	private void printObjectCode(String fileName) {
		// TODO Auto-generated method stub
		File file = new File(fileName);
        BufferedWriter bufferedWriter;
		Token token = null;
		String str = null;
		String text = "";
		int start = 0; //line의 text 시작
		int end = 0; //line의 text 끝
		try {
			bufferedWriter = new BufferedWriter(new FileWriter(file));
			for (int i = 0; i < TokenList.size(); i++) {
				for (int j = 0; j < TokenList.get(i).size(); j++) {
					//출력할 토큰 저장
					token = TokenList.get(i).getToken(j);
					//head 출력, start end 값 설정
					if (token.getOperator().equals("START")) {
						start = token.getLocation();
						end = start;
						if(file.isFile() && file.canWrite()){
							bufferedWriter.write(token.getObjectCode());
							bufferedWriter.newLine();
				        }
					}
					else if (token.getOperator().equals("CSECT") || token.getOperator().equals("END")) {
						//head 출력, start end 값 설정
						if (j == 0) {
							start = token.getLocation();
							end = start;
							if(file.isFile() && file.canWrite()){
								bufferedWriter.write(token.getObjectCode());
								bufferedWriter.newLine();
					        }							
						}
						else {
							//text에 값이 있으면 text 라인 정보를 생성하고 text 출력
							if (text.length() > 0) {
								text = String.format("T%06X%02X", start, end - start - 1).concat(text);
								if(file.isFile() && file.canWrite()){
									bufferedWriter.write(text);
									bufferedWriter.newLine();
						        }
								start = end;
								text = "";
							}
							//modify 출력
							if(file.isFile() && file.canWrite()){
								bufferedWriter.write(token.getObjectCode());
					        }
							//End 출력
							if (TokenList.get(i).getToken(0).getOperator().equals("START"))
								str = String.format("E%06X", token.getLocation() - TokenList.get(i).getToken(0).getLocation());
							else
								str = "E";
							if(file.isFile() && file.canWrite()){
								bufferedWriter.write(str);
								bufferedWriter.newLine();
					        }
								
						}
					}
					//resw이나 resb 토큰이면 text에 값이 있는 경우 text 라인 정보를 생성하고 text 출력
					else if (token.getOperator().equals("RESW") || token.getOperator().equals("RESB")) {
						if (text.length() > 0) {
							text = String.format("T%06X%02X", start, end - start).concat(text);
							if(file.isFile() && file.canWrite()){
								bufferedWriter.write(text);
								bufferedWriter.newLine();
					        }
							start = end;
							text = "";
						}
						start = end + token.getByteSize();
					}
					//extref extdef 코드 출력
					else if (token.getOperator().equals("EXTREF") || token.getOperator().equals("EXTDEF")) {
						if(file.isFile() && file.canWrite()){
							bufferedWriter.write(token.getObjectCode());
							bufferedWriter.newLine();
				        }
					}
					//inst에 있는 operator 토큰
					else if (token.getOperator().equals("EQU") == false){
						//text가 0x1D 이상이면 text 라인 정보를 생성하고 출력
						if (end - start >= 0x1D) {
							text = String.format("T%06X%02X", start, end - start).concat(text);
							if(file.isFile() && file.canWrite()){
								bufferedWriter.write(text);
								bufferedWriter.newLine();
					        }
							start = end;
							text = "";
						}
						//text에 코드를 저장
						else
							text = text.concat(token.getObjectCode());
					}
					//text쓴 만큼 증가
					end += token.getByteSize();
				}
				//object 끝나면 개행
				if(file.isFile() && file.canWrite()){
					bufferedWriter.newLine();
		        }
			}
			bufferedWriter.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	
	/**
	 * 작성된 SymbolTable들을 출력형태에 맞게 출력한다.<br>
	 * @param fileName : 저장되는 파일 이름
	 */
	private void printSymbolTable(String fileName) {
		// TODO Auto-generated method stub
		File file = new File(fileName);
        BufferedWriter bufferedWriter;
		try {
			bufferedWriter = new BufferedWriter(new FileWriter(file));
			for (int i = 0; i < symtabList.size(); i++) {
				SymbolTable symTab = symtabList.get(i);
				for (int j = 0; j < symTab.length(); j++) {
					if(file.isFile() && file.canWrite()){
						bufferedWriter.write(symTab.getSymbol(j) + "\t" + Integer.toString(symTab.getLocation(j), 16).toUpperCase());
						bufferedWriter.newLine();
			        }
				}
				bufferedWriter.newLine();
			}
			bufferedWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/** 
	 * pass1 과정을 수행한다.<br>
	 *   1) 프로그램 소스를 스캔하여 토큰단위로 분리한 뒤 토큰테이블 생성<br>
	 *   2) label을 symbolTable에 정리<br>
	 *   <br><br>
	 *    주의사항 : SymbolTable과 TokenTable은 프로그램의 section별로 하나씩 선언되어야 한다.
	 */
	private void pass1() {
		// TODO Auto-generated method stub
		SymbolTable symTab = new SymbolTable();
		TokenTable tokenTab = new TokenTable(symTab, instTable);
		int returnValue;
		for (int i = 0; i < lineList.size(); i++) {
			returnValue = tokenTab.putToken(lineList.get(i));
			//return값이 1이나 2면 오브젝트 끝으로 새로운 토큰리스트를 만든다
			if (returnValue == 1) {
				if (symTab.length() > 1) {
					symtabList.add(symTab);
					TokenList.add(tokenTab);
					symTab = new SymbolTable();
					tokenTab = new TokenTable(symTab, instTable);
					if (returnValue == 1)
						i--;
				}
			}
		}
	}
	
	/**
	 * pass2 과정을 수행한다.<br>
	 *   1) 분석된 내용을 바탕으로 object code를 생성하여 codeList에 저장.
	 */
	private void pass2() {
		// TODO Auto-generated method stub
		for (int i = 0; i < TokenList.size(); i++) {
			for (int j = 0; j < TokenList.get(i).size(); j++) {
				TokenList.get(i).makeObjectCode(j);
			}
		}
	}
	
	/**
	 * inputFile을 읽어들여서 lineList에 저장한다.<br>
	 * @param inputFile : input 파일 이름.
	 */
	private void loadInputFile(String inputFile) {
		// TODO Auto-generated method stub
		String str;
		try {
			File file = new File(inputFile);
			FileReader filereader = new FileReader(file);
			BufferedReader bufReader = new BufferedReader(filereader);
			
			while((str = bufReader.readLine()) != null){
				lineList.add(str);
            }           
			
			bufReader.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
