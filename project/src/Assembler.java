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
 * �� ���α׷��� SIC/XE �ӽ��� ���� Assembler ���α׷��� ���� ��ƾ�̴�.
 * ���α׷��� ���� �۾��� ������ ����. <br>
 * 1) ó�� �����ϸ� Instruction ���� �о�鿩�� assembler�� �����Ѵ�. <br>
 * 2) ����ڰ� �ۼ��� input ������ �о���� �� �����Ѵ�. <br>
 * 3) input ������ ������� �ܾ�� �����ϰ� �ǹ̸� �ľ��ؼ� �����Ѵ�. (pass1) <br>
 * 4) �м��� ������ �������� ��ǻ�Ͱ� ����� �� �ִ� object code�� �����Ѵ�. (pass2) <br>
 * 
 * <br><br>
 * �ۼ����� ���ǻ��� : <br>
 *  1) ���ο� Ŭ����, ���ο� ����, ���ο� �Լ� ������ �󸶵��� ����. ��, ������ ������ �Լ����� �����ϰų� ������ ��ü�ϴ� ���� �ȵȴ�.<br>
 *  2) ���������� �ۼ��� �ڵ带 �������� ������ �ʿ信 ���� ����ó��, �������̽� �Ǵ� ��� ��� ���� ����.<br>
 *  3) ��� void Ÿ���� ���ϰ��� ������ �ʿ信 ���� �ٸ� ���� Ÿ������ ���� ����.<br>
 *  4) ����, �Ǵ� �ܼ�â�� �ѱ��� ��½�Ű�� �� ��. (ä������ ����. �ּ��� ���Ե� �ѱ��� ��� ����)<br>
 * 
 * <br><br>
 *  + �����ϴ� ���α׷� ������ ��������� �����ϰ� ���� �е��� ������ ��� �޺κп� ÷�� �ٶ��ϴ�. ���뿡 ���� �������� ���� �� �ֽ��ϴ�.
 */
public class Assembler {
	/** instruction ���� ������ ���� */
	InstTable instTable;
	/** �о���� input ������ ������ �� �� �� �����ϴ� ����. */
	ArrayList<String> lineList;
	/** ���α׷��� section���� symbol table�� �����ϴ� ����*/
	ArrayList<SymbolTable> symtabList;
	/** ���α׷��� section���� ���α׷��� �����ϴ� ����*/
	ArrayList<TokenTable> TokenList;
	/** 
	 * Token, �Ǵ� ���þ ���� ������� ������Ʈ �ڵ���� ��� ���·� �����ϴ� ����. <br>
	 * �ʿ��� ��� String ��� ������ Ŭ������ �����Ͽ� ArrayList�� ��ü�ص� ������.
	 */
	ArrayList<String> codeList;
	
	/**
	 * Ŭ���� �ʱ�ȭ. instruction Table�� �ʱ�ȭ�� ���ÿ� �����Ѵ�.
	 * 
	 * @param instFile : instruction ���� �ۼ��� ���� �̸�. 
	 */
	public Assembler(String instFile) {
		instTable = new InstTable(instFile);
		lineList = new ArrayList<String>();
		symtabList = new ArrayList<SymbolTable>();
		TokenList = new ArrayList<TokenTable>();
		codeList = new ArrayList<String>();
	}

	/** 
	 * ��U���� ���� ��ƾ
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
	 * �ۼ��� codeList�� ������¿� �°� ����Ѵ�.<br>
	 * @param fileName : ����Ǵ� ���� �̸�
	 */
	private void printObjectCode(String fileName) {
		// TODO Auto-generated method stub
		File file = new File(fileName);
        BufferedWriter bufferedWriter;
		Token token = null;
		String str = null;
		String text = "";
		int start = 0; //line�� text ����
		int end = 0; //line�� text ��
		try {
			bufferedWriter = new BufferedWriter(new FileWriter(file));
			for (int i = 0; i < TokenList.size(); i++) {
				for (int j = 0; j < TokenList.get(i).size(); j++) {
					//����� ��ū ����
					token = TokenList.get(i).getToken(j);
					//head ���, start end �� ����
					if (token.getOperator().equals("START")) {
						start = token.getLocation();
						end = start;
						if(file.isFile() && file.canWrite()){
							bufferedWriter.write(token.getObjectCode());
							bufferedWriter.newLine();
				        }
					}
					else if (token.getOperator().equals("CSECT") || token.getOperator().equals("END")) {
						//head ���, start end �� ����
						if (j == 0) {
							start = token.getLocation();
							end = start;
							if(file.isFile() && file.canWrite()){
								bufferedWriter.write(token.getObjectCode());
								bufferedWriter.newLine();
					        }							
						}
						else {
							//text�� ���� ������ text ���� ������ �����ϰ� text ���
							if (text.length() > 0) {
								text = String.format("T%06X%02X", start, end - start - 1).concat(text);
								if(file.isFile() && file.canWrite()){
									bufferedWriter.write(text);
									bufferedWriter.newLine();
						        }
								start = end;
								text = "";
							}
							//modify ���
							if(file.isFile() && file.canWrite()){
								bufferedWriter.write(token.getObjectCode());
					        }
							//End ���
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
					//resw�̳� resb ��ū�̸� text�� ���� �ִ� ��� text ���� ������ �����ϰ� text ���
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
					//extref extdef �ڵ� ���
					else if (token.getOperator().equals("EXTREF") || token.getOperator().equals("EXTDEF")) {
						if(file.isFile() && file.canWrite()){
							bufferedWriter.write(token.getObjectCode());
							bufferedWriter.newLine();
				        }
					}
					//inst�� �ִ� operator ��ū
					else if (token.getOperator().equals("EQU") == false){
						//text�� 0x1D �̻��̸� text ���� ������ �����ϰ� ���
						if (end - start >= 0x1D) {
							text = String.format("T%06X%02X", start, end - start).concat(text);
							if(file.isFile() && file.canWrite()){
								bufferedWriter.write(text);
								bufferedWriter.newLine();
					        }
							start = end;
							text = "";
						}
						//text�� �ڵ带 ����
						else
							text = text.concat(token.getObjectCode());
					}
					//text�� ��ŭ ����
					end += token.getByteSize();
				}
				//object ������ ����
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
	 * �ۼ��� SymbolTable���� ������¿� �°� ����Ѵ�.<br>
	 * @param fileName : ����Ǵ� ���� �̸�
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
	 * pass1 ������ �����Ѵ�.<br>
	 *   1) ���α׷� �ҽ��� ��ĵ�Ͽ� ��ū������ �и��� �� ��ū���̺� ����<br>
	 *   2) label�� symbolTable�� ����<br>
	 *   <br><br>
	 *    ���ǻ��� : SymbolTable�� TokenTable�� ���α׷��� section���� �ϳ��� ����Ǿ�� �Ѵ�.
	 */
	private void pass1() {
		// TODO Auto-generated method stub
		SymbolTable symTab = new SymbolTable();
		TokenTable tokenTab = new TokenTable(symTab, instTable);
		int returnValue;
		for (int i = 0; i < lineList.size(); i++) {
			returnValue = tokenTab.putToken(lineList.get(i));
			//return���� 1�̳� 2�� ������Ʈ ������ ���ο� ��ū����Ʈ�� �����
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
	 * pass2 ������ �����Ѵ�.<br>
	 *   1) �м��� ������ �������� object code�� �����Ͽ� codeList�� ����.
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
	 * inputFile�� �о�鿩�� lineList�� �����Ѵ�.<br>
	 * @param inputFile : input ���� �̸�.
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
