# Nothing of the app's own needs keeping by name. What is saved to the phone is written and read
# through enum names (the stopwatch states and colours), which R8 leaves as they are, and through
# JSON keys that are plain strings. Nothing is looked up by reflection.
