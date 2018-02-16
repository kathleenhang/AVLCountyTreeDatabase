import java.util.Iterator;
import java.util.Scanner;
import java.io.FileReader;
import java.io.IOException;
import java.io.BufferedReader;

public class main {

	public static void main(String[] args) throws IOException
	{
		int input = 0;
		
		FileReader fileRead = new FileReader("p4small.txt");
		Scanner fileScan = new Scanner(fileRead);
		Scanner scan = new Scanner(System.in);
		
		AVLTreeMap tree = new AVLTreeMap();
		
		//Create the tree using the file
		createTree(fileScan, tree);

		System.out.println("0. Show Tree\n"
				+ "1. Search for a record\n"
				+ "2. Insert a record\n"
				+ "3. Delete a record\n"
				+ "4. List all records\n"
				+ "5. Exit");
		
		while(input != 5)
		{
			System.out.println("\nPlease type 1-5 to make a selection");
			input = scan.nextInt();
			
			if(input == 0)
			{
				tree.drawTree();
			}
			
			//searches for a record
			else if(input == 1)
			{
				System.out.println("Please type the county/state code you want to search");
				int code = scan.nextInt();
			
				if(!search(code, tree))
					System.out.println("The code you entered cannot be found!");
			}
			//inserts a record into the tree
			else if(input == 2)
			{
				int code;
				int population;
				String name;
				
				System.out.println("Please type the county/state code, population, and county/state name");
				code = scan.nextInt();
				population = scan.nextInt();
			
				name = scan.nextLine();
				name = name.trim();
				
				insert(tree, code, population, name);
			}
			//deletes record from tree
			else if(input == 3)
			{
				System.out.println("Please type the code of record to be deleted");
				int code = scan.nextInt();
				
				delete(tree, code);
			}
			//shows all the tree records
			else if(input == 4)
			{
				showAll(tree);
			}
			else if(input == 5)
				System.out.println("Program will now exit");
			else
				System.out.println("Please make a valid selection");
		}
	}

	//creates the tree at the beginning of the program
	public static void createTree(Scanner file, AVLTreeMap avlTree)
	{
		int count = 0;
		
		while(file.hasNext())
		{	
			count++;
			
			file.useDelimiter(",");
			
			int code = file.nextInt();
			int population = file.nextInt();
			
			Scanner s = new Scanner(file.nextLine());
			
			s.useDelimiter("\"");
			s.next();
			
			String name = s.next();
			
			County county = new County(code, population, name);
			
			avlTree.put(code, county);
		}
		System.out.println("AVLTree has been created\n");
	}
	
	//searches for the tree if choice is 1
	public static boolean search(int code, AVLTreeMap avlTree)
	{
		if(avlTree.get(code) != null)
		{
				System.out.println(avlTree.getTime(code));
				return true;
		}
		
		return false;
	}
	
	//adds a record to the tree if choice is 2
	public static void insert(AVLTreeMap avlTree, int code, int population, String name)
	{
		County county = new County(code, population, name);
		
		avlTree.putTime(code, county);

		System.out.println("Record has been added");
	}
	
	
	//removes a record from the tree if the choice is 3
	public static void delete(AVLTreeMap avlTree, int code)
	{
		avlTree.remove(code);
		
		System.out.println("Record has been deleted");
	}
	
	//displays all records from the tree if the choice is 4
	public static void showAll(AVLTreeMap avlTree)
	{
		Iterable<County> iterable =  avlTree.values();
		Iterator<County> iterator = iterable.iterator();
		
		while(iterator.hasNext())
			System.out.println(iterator.next());
	}
}
