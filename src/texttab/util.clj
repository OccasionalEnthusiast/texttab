(ns texttab.util
  (:require [clojure.string :as s]))



(defn tt-values
  "Output a texttab definition of the supplied list of vector values.
  Options:
  :cols <int> (number of columns - default 1)
  :fmt  <str> (format string for floats - default '%.2f')"
  [vlst & opts]
  (let [opts (apply hash-map opts)
        opts (merge {:cols 1
                     :fmt "%.2f"}
                    opts)
        rcnt (int (Math/ceil (/ (count vlst) (:cols opts)))) ; no. of rows
        rows (vec (repeat rcnt ["td"]))
        fld (fn [x] (cond
                      (keyword? x) (name x)
                      (float? x) (format (:fmt opts) x)
                      :else (str x)))
        rows (loop [r 0, [vs & vrest] vlst, rows rows]
               (if (nil? vs)
                 rows
                 (let [ent (s/join " | " (map fld vs))]
                   (if (= r (dec rcnt))
                     (recur 0 vrest (update rows r conj ent))
                     (recur (inc r) vrest (update rows r conj ent))))))
        lines (map #(s/join " | " %) rows)]
    (s/join "\n" lines)))


(defn tt-table
  "Output a texttab definition in table format from the supplied map.
  The map must have k=[row-key, col-key], v=cell-data format.
  Options:
  :r-keys <list> (list of keys to include for the rows)
  :r-sort <comp> (eg. < or >) if not specified rows are not sorted
  :r-fmtfn <fn> format row heading (fn of one variable returing a string)
  :c-keys <list> (list of keys to include for the cols)
  :c-sort <comp> (eg. < or >) if not specified cols are not sorted
  :c-fmtfn <fn> format col heading (fn of one variable returing a string)
  :fmt  <str> (format string for floats - default '%.2f')"
  [m & opts]
  (let [opts (apply hash-map opts)
        label (fn [x] (cond
                        (keyword? x) (name x)
                        (float? x) (format (:fmt opts) x)
                        :else (str x)))
        opts (merge {:fmt "%.2f"
                     :r-keys nil
                     :r-sort nil
                     :r-fmtfn label
                     :c-keys nil
                     :c-sort nil
                     :c-fmtfn label}
                    opts)
        row-ks (if (:r-sort opts)
                 (sort (:r-keys opts) (distinct (map first (keys m))))
                 (distinct (map first (keys m))))
        row-ks (if (:r-keys opts)
                 (filter (set (:r-keys opts)) row-ks)
                 row-ks)
        col-ks (if (:c-sort opts)
                 (sort (:c-sort opts) (distinct (map second (keys m))))
                 (distinct (map second (keys m))))
        col-ks (if (:c-keys opts)
                 (filter (set (:c-keys opts)) col-ks)
                 col-ks)
        rowh (s/join
               " | "
               (cons "th"
                     (cons ""
                           (for [ck col-ks]
                             ((:c-fmtfn opts) ck)))))
        rows (for [rk row-ks]
               (s/join
                 " | "
                 (cons "td"
                       (cons ((:r-fmtfn opts) rk)
                             (for [ck col-ks]
                               (label (get m [rk ck] "-")))))))
        rows (cons rowh rows)]
    (s/join "\n" rows)))

