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

            if is_jmp(&processed_line) {
                continue
            }

            let translated_line = translate(&mut table, &processed_line, &n);

            // Consumes and prints the current line to the file, along with a newline
            if let Err(why) = file.write((translated_line + "\n").as_bytes()) {
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
fn translate(table: &mut HashMap<String, i32>, line: &str, position: &i32) -> String {
    // Find out if the line is an A-command, a C-command or a Label.
    // Parse, then translate.
    /*
    if starts with @ then value after the @
        if the value is a decimal number within bounds translate to 15-bit binary, append onto 0 and return
        else it's a variable, check SymbolTable and replace
    if c-command:
        split up into dest, comp and jmp
        translate each part separately
        concatenate appropriately

    */

    // todo: add compatability for variables and labels
    if &line[0..1] == "@" { // Checks if line is an a-command or variable
        match &line[1..].parse::<u16>() {
            Ok(num) => { // a-command
                if *num <= 32767 { // if the a-command value isn't 0..=32767 then it is treated like a variable
                    return format!("0{num:015b}");
                }
            }
            _ => { // variable
                
            }
        }
        line.to_string()
    } else { // for if it is a c-command
        // split up into individual components and translate
        translate_c_command(&line)
    }
}

fn translate_c_command(command: &str) -> String {
    // Split up into component pieces
    let (mut dest, comp, mut jmp) = parse_c_command(command);

    dest = translate_dest(&dest);
    let (comp, a) = translate_comp(&comp);
    jmp = translate_jmp(&jmp);

    format!("111{}{}{}{}", a, dest, comp, jmp)
}

fn parse_c_command(command: &str) -> (String, String, String) {
    // dest = comp ; jump

    // split by =
    let split_command = &mut command.trim().split("=");
    // if = then this is dest, else comp ; jmp
    let mut dest = String::from(split_command.next().unwrap_or("").trim());
    // if no = then this is comp ; jmp else None
    let mut comp_jmp = String::from(split_command.next().unwrap_or("").trim());
    
    // Swaps dest and comp_jmp if comp_jmp is None
    if comp_jmp == "" {
        comp_jmp = dest;
        dest = String::new();
    }

    // split by ;
    let split_command = &mut comp_jmp.trim().split(";");
    // if there is a ; then before the ; is comp, after is jmp
    let comp = String::from(split_command.next().unwrap_or("").trim());
    // if there isn't a ; then it's just comp
    let jmp = String::from(split_command.next().unwrap_or("").trim());

    (dest, comp, jmp)
}

fn translate_dest(dest: &str) -> String {
    match dest {
        "" => String::from("000"),
        "M" => String::from("001"),
        "D" => String::from("010"),
        "MD" => String::from("011"),
        "A" => String::from("100"),
        "AM" => String::from("101"),
        "AD" => String::from("110"),
        "AMD" => String::from("111"),
        _ => panic!("{} is not a valid dest parameter.", dest)
    }
}

fn translate_comp(comp: &str) -> (String, String) {
    match comp {
        "0" => (String::from("101010"), String::from("0")),
        "1" => (String::from("111111"), String::from("0")),
        "-1" => (String::from("111010"), String::from("0")),
        "D" => (String::from("001100"), String::from("0")),
        "A" => (String::from("110000"), String::from("0")),
        "!D" => (String::from("001101"), String::from("0")),
        "!A" => (String::from("110001"), String::from("0")),
        "-D" => (String::from("001111"), String::from("0")),
        "-A" => (String::from("110011"), String::from("0")),
        "D+1" => (String::from("011111"), String::from("0")),
        "A+1" => (String::from("110111"), String::from("0")),
        "D-1" => (String::from("001110"), String::from("0")),
        "A-1" => (String::from("110010"), String::from("0")),
        "D+A" => (String::from("000010"), String::from("0")),
        "D-A" => (String::from("010011"), String::from("0")),
        "A-D" => (String::from("000111"), String::from("0")),
        "D&A" => (String::from("000000"), String::from("0")),
        "D|A" => (String::from("010101"), String::from("0")),

        "M" => (String::from("110000"), String::from("1")),
        "!M" => (String::from("110001"), String::from("1")),
        "-M" => (String::from("110011"), String::from("1")),
        "M+1" => (String::from("110111"), String::from("1")),
        "M-1" => (String::from("110010"), String::from("1")),
        "D+M" => (String::from("000010"), String::from("1")),
        "D-M" => (String::from("010011"), String::from("1")),
        "M-D" => (String::from("000111"), String::from("1")),
        "D&M" => (String::from("000000"), String::from("1")),
        "M|M" => (String::from("010101"), String::from("1")),

        _ => panic!("{} is not a valid comp parameter.", comp)
    }
}

fn translate_jmp(jmp: &str) -> String {
    match jmp {
        "" => String::from("000"),
        "JGT" => String::from("001"),
        "JEQ" => String::from("010"),
        "JGE" => String::from("011"),
        "JLT" => String::from("100"),
        "JNE" => String::from("101"),
        "JLE" => String::from("110"),
        "JMP" => String::from("111"),

        _ => panic!("{} is not a valid jump parameter.", jmp)
    }
}
