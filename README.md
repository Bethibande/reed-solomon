# reed-solomon
This is a very naive implementation of erasure coding using reed-solomon.
A basic paper on how reed-solomon works can be found here:
https://web.eecs.utk.edu/~jplank/plank/papers/CS-96-332.pdf

The EncodeDemo.java file will encode a given file into data and parity shards.
By default this is a file named ``example.png``. All data and parity shards will be written to the filesystem in the `./data` directory.

The DecodeDemo.java will then decode the encoded file into a ``decoded.png`` file.
Any missing data or parity shards will be restored and written back to the filesystem.
If too many data or parity shards are missing, the decoding will abort and throw an exception.

Please note that this implementation is not optimized for speed or memory usage.