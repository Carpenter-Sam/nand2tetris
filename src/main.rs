use std::collections::HashMap;
use std::env;
use std::fs::File;
use std::io::{self, BufRead};
use std::path::Path;

fn main() {
    let args: Vec<String> = env::args().collect();

    assert!(
        args.len() > 1,
        "Must provide an argument corresponding to a relative filename."
    );

    if let Ok(lines) = read_lines(&args[1]) {
        for line in lines.map_while(Result::ok) {
            match whitespace_remove(&line) {
                Some(new_line) => println!("{}", new_line),
                None => continue,
            }
        }
    }

    // if let Ok(lines) = read_lines(&args[1]) {
    //     for line in lines.map_while(Result::ok) {
    //         println!("{}", line);
    //     }
    // }
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
fn whitespace_remove(line: &str) -> Option<String> {
    let split_line = &mut line.split("//");
    let new_line = String::from(split_line.next().unwrap_or(""));

    if new_line.len() > 0 {
        Some(new_line)
    } else {
        None
    }
}

// Convert instruction into binary
