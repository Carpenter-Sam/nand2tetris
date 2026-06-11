use std::collections::HashMap;
use std::env;
use std::fs::File;
use std::io::{self, BufRead, Write};
use std::path::Path;

fn main() {
    let args: Vec<String> = env::args().collect();

    // Check if filename has been provided.
    assert!(
        args.len() > 1,
        "Must provide an argument corresponding to a relative filename."
    );

    // This is the SymbolTable.
    let mut table: HashMap<String, i32> = HashMap::new();

    if let Ok(lines) = read_lines(&args[1]) {
        let mut n = 0;
        for line in lines.map_while(Result::ok) {
            let processed_line;

            // Remove whitespace and comments, if line is now empty we continue.
            match whitespace_comment_remove(&line) {
                Some(new_line) => processed_line = new_line,
                None => continue,
            }

            if jmp_label_process(&mut table, &processed_line, &n) {
                continue;
            }

            // POTENTIAL ERROR: Currently increases line count if not empty or jump label, 
            // and jump labels look at the next command.
            n += 1;
        }
    }

    println!("{:?}", table);

    // Create a file
    let path = Path::new("output.txt");
    let display = path.display();

    // Open file in write-only mode
    let mut file = match File::create(&path) {
        Err(why) => panic!("couldn't create {}: {}", display, why),
        Ok(file) => file,
    };

    if let Ok(lines) = read_lines(&args[1]) {
        let mut n = 0;
        for line in lines.map_while(Result::ok) {
            let processed_line;

            // Remove whitespace and comments, if line is now empty we continue.
            match whitespace_comment_remove(&line) {
                Some(new_line) => processed_line = new_line,
                None => continue,
            }

            // Consumes and prints the current line to the file, along with a newline
            if let Err(why) = file.write((processed_line + "\n").as_bytes()) {
                panic!("couldn't write to {}: {}", display, why);
            }

            n += 1;
        }
    }
}

fn read_lines<P>(filename: P) -> io::Result<io::Lines<io::BufReader<File>>>
where
    P: AsRef<Path>,
{
    let file = File::open(filename)?;
    Ok(io::BufReader::new(file).lines())
}

// Return symbol look-up table
fn return_symbol(table: &mut HashMap<String, i32>, symbol: String, position: i32) -> i32 {
    match table.get(&symbol) {
        Some(line_number) => *line_number,
        None => {
            table.insert(symbol, position);
            position
        }
    }
}

// Parser (Split up string into subsequent parts)
// fn parser(symbol: String) -> (str, str, str) {

// }

// Removes comments from the line and returns a new line
fn whitespace_comment_remove(line: &str) -> Option<String> {
    let split_line = &mut line.trim().split("//");
    let new_line = String::from(split_line.next().unwrap_or("").trim());
    if new_line.len() > 0 {
        Some(new_line)
    } else {
        None
    }
}

// Get jump label

// Check if jump label, if so process, return boolean
fn jmp_label_process(table: &mut HashMap<String, i32>, line: &str, position: &i32) -> bool {
    println!("{} {}", &position, &line);
    if !is_jmp(line) {
        return false;
    }

    let jmp_label = &line[1..line.len() - 1];
    if table.contains_key(jmp_label) {
        panic!("Jump label used twice.")
    } else {
        table.insert(String::from(jmp_label), *position);
    }

    true
}

// Check if jump label
fn is_jmp(line: &str) -> bool {
    if line.is_empty() {
        return false;
    }

    if line.chars().next() != Some('(') || line.chars().next_back() != Some(')') {
        return false;
    }

    true
}

// Convert instruction into binary


// Enter in a line, and it will return a translation
fn translate(line: &str) -> String {
    // Find out if the line is an A-command, a C-command or a Label.
    // Parse, then translate.
    /*
    if starts with @ then value after the @
        if the value is a decimal number within bounds translate to 15-bit binary, append onto 0 and return
        else it's a label, check SymbolTable and replace
    if c-command:
        split up into dest, comp and jmp
        translate each part separately
        concatenate appropriately

    */

}
