import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class Indexes {
	//will store the name of the folders or the name of the image file (the index in the list will be the correct index of the image file)
    ArrayList<String> names;  
    
    //will store 0xFFFFFFFF for folder, the index of the folder for image files
    ArrayList<Long> indexes;
    ArrayList<Long> originalIndexes ;
    String folder;
    Long folderIndex = new Long(4294967295L);
    long entries;
    
    public Indexes(String folder) {
    	names = new ArrayList<>();  
        indexes = new ArrayList<>();
        originalIndexes = new ArrayList<>();
        this.folder = folder;
	}
    
    public void Populate() throws Exception{
    	DataInputStream din = new DataInputStream(new BufferedInputStream(new FileInputStream(folder+"thumbindex.db")));
        @SuppressWarnings("unused")
		long magic = ReadFunctions.readUnsignedInt(din); //file start with a magic sequence
        entries = ReadFunctions.readUnsignedInt(din); // then number of entries

        System.out.println("nb entries: "+entries);
        
        String path;
        long index;
        Long folderIndex = new Long(4294967295L); //0xFFFFFFFF
        
       for(int i=0;i<entries;i++){
           
            path = ReadFunctions.getString(din);  // null terminated string
            ReadFunctions.read26(din);            // followed by 26 garbaged bytes
            index = ReadFunctions.readUnsignedInt(din); // followed by the index (0xFFFFFFFF for folder)
            
            
            names.add(path);
            indexes.add(index);
            originalIndexes.add(index);
            
            
            if(path.equals("")){   //empty file name (deleted), change index to 0xFFFFFFFF
                indexes.set(i, folderIndex);
                continue;
            }

        
       }
        din.close();
    }
    
    public void writeCSV(String output) throws IOException{
    	StringBuilder s = new StringBuilder("Index;Original Indexes;type;Image Path\n");
        for(int i=0; i<entries; i++){
            if(indexes.get(i).compareTo(folderIndex)!=0){ // not a folder
                s.append(i+";"+originalIndexes.get(i)+";0;"+names.get(new Long(indexes.get(i)).intValue()) +names.get(i)+"\n");
            }else{ // folder
            	if(names.get(i).equals("")){
            		s.append(i+";"+originalIndexes.get(i)+";2;" +names.get(i)+"\n");
            	}else{
            		s.append(i+";"+originalIndexes.get(i)+";1;" +names.get(i)+"\n");
            	}
            }
        }

        FileWriter fw = new FileWriter(output+"indexes.csv");
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write(s.toString());
        bw.close();
    }
    
    @SuppressWarnings("static-access")
	public static void main(String []args) throws Exception{
    	Options options = new Options();
    	options.addOption("h","help", false, "prints the help content");
    	options.addOption(OptionBuilder.withArgName("srcFolder").hasArg().isRequired().withDescription("Picasa DB folder").create("folder"));
    	options.addOption(OptionBuilder.withArgName("outputFolder").hasArg().isRequired().withDescription("output folder").create("output"));
    	
    	CommandLineParser parser = new GnuParser();
    	String folder=null;
    	String output=null;
        try {
            // parse the command line arguments
            CommandLine line = parser.parse( options, args );
            if(line.hasOption("h")){
            	HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp( "ReadThumbs" , options );
                System.exit(1);
            }
            if(line.hasOption("folder")){
            	folder = line.getOptionValue("folder");
                if(!folder.endsWith(File.separator)){
                	folder += File.separator;
                }
            	if(! new File(folder).exists()){
            		throw new Exception("Source folder does not exist:"+folder);
            	}
            }
            if(line.hasOption("output")){
            	output = line.getOptionValue("output");
                if(!output.endsWith(File.separator)){
                	output += File.separator;
                }
            	if(! new File(output).exists()){
            		throw new Exception("output folder does not exist:"+output);
            	}
            }
        }
        catch( ParseException exp ) {
            // oops, something went wrong
        	
            System.err.println( "Parsing failed.  Reason: " + exp.getMessage() );
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp( "ReadThumbs" , options );
            System.exit(1);
        }
        
        Indexes indexes = new Indexes(folder);
        indexes.Populate();
        indexes.writeCSV(output);
    }
}
