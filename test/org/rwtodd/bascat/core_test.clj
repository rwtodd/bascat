(ns org.rwtodd.bascat.core-test
  (:import (java.nio ByteBuffer ByteOrder BufferUnderflowException))
  (:require [clojure.test :refer :all]
            [org.rwtodd.bascat.core :refer :all]))

(defn- make-buffer
  "create a test ByteBuffer"
  [xs]
  (-> (byte-array xs)
      ByteBuffer/wrap
      (.order ByteOrder/LITTLE_ENDIAN)))

(deftest mbf-tests
  (testing "MBF-zero-case"
    (is (== 0.0 (#'org.rwtodd.bascat.core/decode-mbf32 (make-buffer [0 0 0 0]))))
    (is (== 0.0 (#'org.rwtodd.bascat.core/decode-mbf32 (make-buffer [1 2 3 0]))))
    (is (== 0.0 (#'org.rwtodd.bascat.core/decode-mbf64 (make-buffer [0 0 0 0 0 0 0 0]))))
    (is (== 0.0 (#'org.rwtodd.bascat.core/decode-mbf64 (make-buffer [1 2 3 4 5 6 7 0])))))
  (testing "MBF32-wikipedia-examples"
    (is (== 10 (#'org.rwtodd.bascat.core/decode-mbf32 (make-buffer [0 0 0x20 0x84]))))
    (is (== 1 (#'org.rwtodd.bascat.core/decode-mbf32 (make-buffer [0 0 0 0x81]))))
    (is (== 0.5 (#'org.rwtodd.bascat.core/decode-mbf32 (make-buffer [0 0 0 0x80]))))
    (is (== 0.25 (#'org.rwtodd.bascat.core/decode-mbf32 (make-buffer [0 0 0 0x7f]))))
    (is (== -0.5 (#'org.rwtodd.bascat.core/decode-mbf32 (make-buffer [0 0 0x80 0x80]))))
    (is (== (float (Math/sqrt 0.5))
            (#'org.rwtodd.bascat.core/decode-mbf32 (make-buffer [0xf3 0x04 0x35 0x80]))))
    (is (== (float (Math/sqrt 2.0))
            (#'org.rwtodd.bascat.core/decode-mbf32 (make-buffer [0xf3 0x04 0x35 0x81]))))
    (is (== (float (/ Math/PI 2.0))
            (#'org.rwtodd.bascat.core/decode-mbf32 (make-buffer [0xdb 0x0f 0x49 0x81])))))
  (testing "MBF64-basics"
    (is (== 10 (#'org.rwtodd.bascat.core/decode-mbf64 (make-buffer [0 0 0 0 0 0 0x20 0x84]))))
    (is (== 1 (#'org.rwtodd.bascat.core/decode-mbf64 (make-buffer [0 0 0 0 0 0 0 0x81]))))
    (is (== 0.5 (#'org.rwtodd.bascat.core/decode-mbf64 (make-buffer [0 0 0 0 0 0 0 0x80]))))
    (is (== 0.25 (#'org.rwtodd.bascat.core/decode-mbf64 (make-buffer [0 0 0 0 0 0 0 0x7f]))))
    (is (== -0.5 (#'org.rwtodd.bascat.core/decode-mbf64 (make-buffer [0 0 0 0 0 0 0x80 0x80]))))))


(defn- gwbasic-lines
  "Generate a tokenized array from a collection of tokenized lines.
  Lines will be numbered 1, 2, etc..."
  [& ls]
  (byte-array
   (concat [255]
           (apply concat
                  (interleave (map #(vector 1 0 (inc %) 0) (range))
                              ls
                              (repeat [0])))
           [0 0])))

(deftest bascat-tests
  (testing "Malformed file"
    (is (thrown? IllegalArgumentException (bascat (byte-array [77 1 0 0x91 0])))))
  (testing "Unexpected EOF"
    (is (thrown? BufferUnderflowException (bascat (byte-array [255 1 0 0x91 0])))))
  (testing "tiny hand-made examples"
    (is (= (format "1  PRINT%n")
           (with-out-str (bascat (gwbasic-lines [0x91])))))
    (is (= (format "1  PRINT%n2  GOTO 1%n")
           (with-out-str (bascat (gwbasic-lines [0x91] [0x89 0x20 0x12])))))))
