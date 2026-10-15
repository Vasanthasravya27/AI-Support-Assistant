import unittest

from app.chunker import chunk_text, extract_title


class ChunkerTest(unittest.TestCase):
    def test_short_text_is_single_chunk(self):
        self.assertEqual(chunk_text("Hello world.", 100, 10), ["Hello world."])

    def test_paragraphs_are_packed_up_to_limit(self):
        text = "A" * 40 + "\n\n" + "B" * 40 + "\n\n" + "C" * 40
        chunks = chunk_text(text, max_chars=90, overlap=0)
        self.assertEqual(len(chunks), 2)
        self.assertIn("A" * 40, chunks[0])
        self.assertIn("B" * 40, chunks[0])
        self.assertEqual(chunks[1], "C" * 40)

    def test_overlap_repeats_tail_of_previous_chunk(self):
        text = "one two three four five six seven eight nine ten.\n\n" + "y" * 60
        chunks = chunk_text(text, max_chars=60, overlap=15)
        self.assertGreaterEqual(len(chunks), 2)
        self.assertIn("nine ten.", chunks[1])

    def test_long_paragraph_is_split_and_bounded(self):
        text = " ".join(["This is a sentence."] * 50)
        chunks = chunk_text(text, max_chars=100, overlap=0)
        self.assertGreater(len(chunks), 5)
        self.assertTrue(all(len(c) <= 100 for c in chunks))

    def test_empty_text_gives_no_chunks(self):
        self.assertEqual(chunk_text("   \n\n  ", 100, 10), [])

    def test_extract_title(self):
        self.assertEqual(extract_title("intro\n# Password Reset\nbody", "f.md"), "Password Reset")
        self.assertEqual(extract_title("no heading", "f.md"), "f.md")


if __name__ == "__main__":
    unittest.main()
