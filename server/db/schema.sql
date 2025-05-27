CREATE DATABASE IF NOT EXISTS chess;

USE chess;

CREATE TABLE IF NOT EXISTS users (
    username VARCHAR(255) PRIMARY KEY,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL
    );

CREATE TABLE IF NOT EXISTS auths (
    authToken VARCHAR(255) PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    FOREIGN KEY (username) REFERENCES users(username)
    );

CREATE TABLE IF NOT EXISTS games (
                                     gameID INT AUTO_INCREMENT PRIMARY KEY,
                                     whiteUsername VARCHAR(255),
    blackUsername VARCHAR(255),
    gameName VARCHAR(255) NOT NULL,
    game TEXT NOT NULL,
    FOREIGN KEY (whiteUsername) REFERENCES users(username),
    FOREIGN KEY (blackUsername) REFERENCES users(username)
    );