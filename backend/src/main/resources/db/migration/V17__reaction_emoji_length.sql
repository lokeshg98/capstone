-- Support ZWJ / skin-tone emoji in reactions (JoyPixels unicode sequences)

ALTER TABLE reactions ALTER COLUMN emoji TYPE VARCHAR(32);
