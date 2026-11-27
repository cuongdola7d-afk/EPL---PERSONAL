-- Wordle 2026/27 curated name overrides (19 players).
-- Idempotent upsert: safe to run repeatedly on both MySQL 8 and H2 (MODE=MySQL).
-- Every word strictly matches ^[A-Z]+( [A-Z]+)*$ and has 4-11 letters.
-- Calvert-Lewin and Dewsbury-Hall are intentionally NOT overridden (12 letters, excluded).

INSERT INTO wordle_name_overrides (player_id, word, note) VALUES
    (2000003001, 'ALISSON', 'Alisson Becker -> tên thường gọi'),
    (2000001011, 'KEPA', 'Kepa Arrizabalaga -> tên thường gọi (tránh 12 chữ)'),
    (2000030246, 'SAVINHO', 'Sávio Moreira de Oliveira -> tên thường gọi'),
    (2000030217, 'FLORENTINO', 'Florentino Ibrain Morris Luís -> tên thường gọi'),
    (2000030028, 'VAN HECKE', 'Jan Paul van Hecke -> bỏ tên đệm Paul'),
    (2000030163, 'DIOUF', 'El Hadji Malick Diouf -> tên ngắn'),
    (2000020025, 'FATAWU', 'Abdul Fatawu Issahaku -> tên thường gọi'),
    (138931, 'PHILOGENE', 'Jaden Philogene-Bidace -> tên thường gọi'),
    (2000005010, 'GITTENS', 'Jamie Bynoe-Gittens -> tên thường gọi'),
    (2000001006, 'GABRIEL', 'Gabriel Magalhaes -> tên thường gọi'),
    (2000005008, 'JOAO PEDRO', 'Joao Pedro -> giữ đủ tên kép'),
    (2000030006, 'IGOR THIAGO', 'Igor Thiago -> giữ đủ tên kép'),
    (2000020056, 'IGOR JESUS', 'Igor Jesus -> giữ đủ tên kép'),
    (2000030057, 'JOAO GOMES', 'João Gomes -> giữ đủ tên kép'),
    (2000020106, 'LARSEN', 'Jørgen Strand Larsen -> tên ngắn'),
    (2000030154, 'SOLER', 'Julio Soler Barreto -> tên ngắn'),
    (2000030164, 'NUNES', 'Gustavo Nunes Fernandes Gomes -> tên ngắn'),
    (2000030174, 'CHEMA', 'José María Andrés Baixauli -> tên thường gọi'),
    (2000030222, 'JOSEPH', 'Mateo Joseph Fernández-Regatillo -> tên ngắn')
ON DUPLICATE KEY UPDATE
    word = VALUES(word),
    note = VALUES(note);
