README

What This Project Does:

This tool takes a block of text (typed or from a `.txt` file), cleans it up, breaks it into word combinations called N-Grams and counts how often each one appears and exports the results as a CSV and JSON file.



Project Structure:


ngram-generator/
├── src/
│   ├── main/java/com/ngram/
│   │   ├── TextCleaner.java      # This cleans text, turns it into tokens, and generates N-Grams
│   │   ├── Exporter.java         # This counts frequencies and then exports CSV and JSON
│   │   └── Main.java             # Runs the entire program
│   └── test/java/com/ngram/
│       └── NGramTest.java        # JUnit 5 unit tests - used for debugging/checking
└── pom.xml                       # Maven config and dependencies




Requirements:


Java (Temurin) 17 
Maven 
Eclipse IDE


How to Run:

1. Open Eclipse and import the project via File → Open Projects from File System
2. In the Package Explorer, open `Main.java`
3. Press the green Play button
4. The console will appear at the bottom with this menu:


    N-Gram Generator 
1 - Type text manually
2 - Load a .txt file
Choose an option:

Option 1: Type text manually
- Choose `1` and press Enter
- Type your text and press Enter
- Enter your minimum N value 
- Enter your maximum N value 
- Enter the folder path where you want the output saved 

Option 2: Load a .txt file
- Choose `2` and press Enter
- Enter the full path to your `.txt` file
- Enter your N values and output folder as above


Output Files:

After running, two files will be saved in your chosen output folder:

ngrams.csv:

ngram,frequency
full text,25
text search,18


ngrams.json:


{
  "full text": 25,
  "text search": 18
}




Sample Test Cases:

Input:
`"Full text search"`
`"hello hello world"`

N Value:
N=2
N=1

Expected Output:
`"full text"`, `"text search"`
`hello = 2`, `world = 1`

How to Run Unit Tests:

1. Open `NGramTest.java` in Eclipse
2. Right-click anywhere inside the file
3. Select Run As → JUnit Test
4. A green bar means all 4 tests passed


Performance:
Input size supported up to 1 MB 
Processing time under 30 seconds
UTF-8 supported

Processing time is printed to the console after every run.


Design:

Text is converted to lowercase before processing so `"The"` and `"the"` are treated as the same word
All characters except letters, numbers, and spaces are removed before turning into tokens
N-Gram sizes are configurable at runtime
Output files are always named `ngrams.csv` and `ngrams.json` and saved to the folder you specify


Dependencies (pom.xml):

JUnit Jupiter 5.10.0 (Unit testing)
Jackson Databind 2.15.2 (JSON export)
