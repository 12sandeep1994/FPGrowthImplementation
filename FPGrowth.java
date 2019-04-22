package newProgram;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FPGrowth {
	Integer count;
	int item;
	BufferedReader br;
	String[] splitLine;
	int noOfBuckets = 0; 
	int noOfFreqItemSet; 
	int minimumSupport;
	BufferedWriter bw = null; 
	int BUFSIZE = 2000;
	int[] itemBuf = null;
	Node[] nodeBuf = null;
	int[] itemsetOutputBuffer = null;
	int maxPatternLength = 1000;
	HashMap<Integer , Integer> singletonItemCount ;
	FPTree tree;
	
	public FPGrowth() {
	}

	private void onlyProceedWithMinSup(String inputFile, String outputFile) throws IOException {
		bw = new BufferedWriter(new FileWriter(outputFile)); 
		tree = new FPTree();
		BufferedReader br = new BufferedReader(new FileReader(inputFile));
		String line;
		line =  br.readLine();
		for(int i=0; i<noOfBuckets; i++){ 
			line =  br.readLine();
			splitLine = line.split(",");
			List<Integer> usefulItemSets = new ArrayList<Integer>();
			for(int j=1; j<splitLine.length; j++)
			{
				Integer item = Integer.parseInt(splitLine[j]);
				if(singletonItemCount.get(item) >= minimumSupport)
					usefulItemSets.add(item);	
			}
			Collections.sort(usefulItemSets, new Comparator<Integer>(){
				public int compare(Integer item1, Integer item2){
					int compare = singletonItemCount.get(item2) - singletonItemCount.get(item1);
					if(compare == 0){ 
						return (item1 - item2);
					}
					return compare;
				}
			});
			tree.addItemSets(usefulItemSets);
		}
		br.close();
		tree.createHeader(singletonItemCount);
		if(tree.header.size() > 0) {
			itemBuf = new int[BUFSIZE];
			nodeBuf = new Node[BUFSIZE];
			fp(tree, itemBuf, 0, noOfBuckets, singletonItemCount);
		}
		if(bw != null){
			bw.close();
		}
	
	
	}	
	private void findSingletonCount(String inputFile) throws IOException {
		noOfFreqItemSet = 0;
		itemsetOutputBuffer = new int[BUFSIZE];
		br = new BufferedReader(new FileReader(inputFile));
		splitLine = br.readLine().split(" ");
		noOfBuckets = Integer.parseInt(splitLine[0]);
		minimumSupport = Integer.parseInt(splitLine[1]);
		singletonItemCount = new HashMap<>();
		
		for(int i=0; i<noOfBuckets; i++)
		{
			splitLine = br.readLine().split(",");
			for(int j=1; j<splitLine.length; j++)
			{
				item = Integer.parseInt(splitLine[j]);
				count=singletonItemCount.get(item);
				if(count==null)
					singletonItemCount.put(item, 0);
				else
					singletonItemCount.put(item, ++count);
					
			}
		}
		br.close();		
	}

	private void fp(FPTree tree, int [] prefix, int prefixLen, int prefixSupport, Map<Integer, Integer> singletonItemCount) throws IOException {
		
		if(prefixLen == maxPatternLength){
			return;
		}
		boolean oneBranch = true;
		int pos = 0;
		if(tree.root.children.size() > 1) {
			oneBranch = false;
		}else {
			Node currNode = tree.root.children.get(0);
			while(true){
				if(currNode.children.size() > 1) {
					oneBranch = false;
					break;
				}
				nodeBuf[pos] = currNode;				
				pos++;
				if(currNode.children.size() == 0) {
					break;
				}
				currNode = currNode.children.get(0);
			}
		}		
		if(oneBranch){	
			allCombPrefix(nodeBuf, pos, prefix, prefixLen);
		}else {
			for(int i = tree.header.size()-1; i>=0; i--){
				Integer item = tree.header.get(i);
				int support = singletonItemCount.get(item);
				prefix[prefixLen] = item;
				int betaSupport = (prefixSupport < support) ? prefixSupport: support;
				saveSet(prefix, prefixLen+1, betaSupport);			
				if(prefixLen+1 < maxPatternLength){
					List<List<Node>> prefixPaths = new ArrayList<List<Node>>();
					Node path = tree.itemNode.get(item);
					Map<Integer, Integer> singletonItemCountBeta = new HashMap<Integer, Integer>();	
					while(path != null){
						if(path.parent.id != -1){
							List<Node> prefixPath = new ArrayList<Node>();
							prefixPath.add(path);   
							int pathCount = path.counter;
							Node parent = path.parent;
							while(parent.id != -1){
								prefixPath.add(parent);
								if(singletonItemCountBeta.get(parent.id) == null){
									singletonItemCountBeta.put(parent.id, pathCount);
								}else{
									singletonItemCountBeta.put(parent.id, singletonItemCountBeta.get(parent.id) + pathCount);
								}
								parent = parent.parent;
							}
							prefixPaths.add(prefixPath);
						}
						path = path.nodeLink;
					}
					FPTree treeBeta = new FPTree();
					for(List<Node> prefixPath : prefixPaths){
						treeBeta.insertPrefixPath(prefixPath, singletonItemCountBeta, minimumSupport); 
					}  
					if(treeBeta.root.children.size() > 0){
						treeBeta.createHeader(singletonItemCountBeta); 
						fp(treeBeta, prefix, prefixLen+1, betaSupport, singletonItemCountBeta);
					}
				}
			}
		}
		
	}
	private void allCombPrefix(Node[] nodeBuf, int pos, int[] prefix, int prefixLen) throws IOException {

		int support = 0;
loop1:	for (long i = 1, max = 1 << pos; i < max; i++) {
			int newPrefixLength = prefixLen;
			for (int j = 0; j < pos; j++) {
				int isSet = (int) i & (1 << j);
				if (isSet > 0) {
					if(newPrefixLength == maxPatternLength){
						continue loop1;
					}
					
					prefix[newPrefixLength++] = nodeBuf[j].id;
						support = nodeBuf[j].counter;
				}
			}
			saveSet(prefix, newPrefixLength, support);
		}
	}
	private void saveSet(int [] itemset, int itemsetLength, int support) throws IOException {
			
			noOfFreqItemSet++;
			System.arraycopy(itemset, 0, itemsetOutputBuffer, 0, itemsetLength);
			Arrays.sort(itemsetOutputBuffer, 0, itemsetLength);
			StringBuilder buffer = new StringBuilder();
			buffer.append("{");
			buffer.append(itemsetOutputBuffer[0]);
			for(int i=1; i< itemsetLength; i++)
				buffer.append(","+itemsetOutputBuffer[i]);
			buffer.append("}");
			buffer.append(" SUPPORT: ");
			buffer.append(support);
			if(buffer.charAt(3)=='}'||buffer.charAt(2)=='}')
				noOfFreqItemSet--;
			else
				bw.write(buffer.toString()+"\n");
	}
	public static void main(String [] arg) throws FileNotFoundException, IOException{
		String input = ("D:\\FIM\\NewOutput_Comma.txt");  
		String output = "D:\\FIM\\output1.txt";
		FPGrowth ob = new FPGrowth();
		long startTime = new Date().getTime();
		ob.findSingletonCount(input); 
		ob.onlyProceedWithMinSup(input,output);
		long endTime = new Date().getTime();
		System.out.println("Time taken:"+(endTime-startTime)+" ms");
	}
}


class FPTree {
	List<Integer> header = null;
	Map<Integer, Node> itemNode = new HashMap<Integer, Node>();
	Map<Integer, Node> itemNodeEnd = new HashMap<Integer, Node>();
	Node root = new Node();

	public void addItemSets(List<Integer> usefulItemSets) {
		Node currNode = root;
		for(Integer item : usefulItemSets){
			Node child = currNode.getChildID(item);
			if(child == null){ 
				Node newNode = new Node();
				newNode.id = item;
				newNode.parent = currNode;
				currNode.children.add(newNode);
				currNode = newNode;
				fixNodeLinks(item, newNode);	
			}else{ 
				child.counter++;
				currNode = child;
			}
		}
		
	}
	
	public FPTree(){	
		
	}
	private void fixNodeLinks(Integer item, Node newNode) {
		Node lastNode = itemNodeEnd.get(item);
		if(lastNode != null) {
			lastNode.nodeLink = newNode;
		} 
		itemNodeEnd.put(item, newNode); 
		Node headernode = itemNode.get(item);
		if(headernode == null){  // there is not
			itemNode.put(item, newNode);
		}
	}
	void insertPrefixPath(List<Node> prefixPath, Map<Integer, Integer> singletonItemCountBeta, int relativeMinsupp) {
		int pathCount = prefixPath.get(0).counter;  
		Node currNode = root;
		for(int i = prefixPath.size() -1; i >=1; i--){ 
			Node pathItem = prefixPath.get(i);
			if(singletonItemCountBeta.get(pathItem.id) >= relativeMinsupp){
				Node child = currNode.getChildID(pathItem.id);
				if(child == null){ 
					Node newNode = new Node();
					newNode.id = pathItem.id;
					newNode.parent = currNode;
					newNode.counter = pathCount;  
					currNode.children.add(newNode);
					currNode = newNode;
					fixNodeLinks(pathItem.id, newNode);		
				}else{ 
					child.counter += pathCount;
					currNode = child;
				}
			}
		}
	}

	void createHeader(Map<Integer, Integer> singletonItemCount) {
		header =  new ArrayList<Integer>(itemNode.keySet());
		Collections.sort(header, new Comparator<Integer>(){
			public int compare(Integer id1, Integer id2){
				int compare = singletonItemCount.get(id2) - singletonItemCount.get(id1);
				if(compare==0)
					return id1-id2;
				return compare;
			}
		});
	}
}

class Node{

	List<Node> children = new ArrayList<Node>();
	int id = -1, counter = 1; 
	Node parent, nodeLink ; 
	Node(){
		
	}
	Node getChildID(int id) {
		for(Node child : children){
			if(child.id == id)
				return child;
		}
		return null;
	}
}
