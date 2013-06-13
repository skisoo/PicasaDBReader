# PicasaDBReader

simple tool to read picasa 3.9 database.

There are 2 simple programs:
* PMPDB
* PicasaFaces

Dependencies:
* commons cli: http://commons.apache.org/proper/commons-cli/download_cli.cgi
* commons io: http://commons.apache.org/proper/commons-io/download_io.cgi

## PMPDB
PMPDB read all the PMP files containing the Picasa Database and the file containing indexes thumbindex.db. This program
will create 3 csv files: albumdata.csv (album database), catdata.csv (category database), imagedata.csv (picture database).

Usage:
```bash
java -classpath ".:bin/:commons-cli-1.2.jar" PMPDB -folder "/path/to/PicasaDB/Picasa2/db3/" -output ./OutputFolder
```

## PicasaFaces
PicasaFaces will extract the face information from the Picasa Database and save it in a csv file. If the 
command line contains the argument "-convert" followed by the path to convert, then imagemagick will create 
all the face thumbshots (in the output folder with a folder for each person). A string replacement of the image paths
can be done if the pictures location is different from the database.

Usage:
```bash
java -classpath ".:bin/:commons-cli-1.2.jar:commons-io-2.4.jar" PicasaFaces -folder "/path/to/PicasaDB/Picasa2/db3/" -output ./OutputFolder -replaceRegex C: -replacement /media/HardDrive -convert /path/to/convert(.exe)
```

update k3b 2013-06-13:
Added support for picasaDB defaultlocation

