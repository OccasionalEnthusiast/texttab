(ns texttab.core
  (:require [hiccup.core :refer [html]]
            [clojure.string :as s]))


(defn- style-to-attr
  "Convert a super-set styles map to an html attribute map"
  [styles]
  (let [attrs {}
        colspan (get styles :colspan)
        attrs (if colspan (assoc attrs :colspan colspan) attrs)
        rowspan (get styles :rowspan)
        attrs (if rowspan (assoc attrs :rowspan rowspan) attrs)
        styles (dissoc styles :colspan :rowspan :format)
        style-str (s/join "; " (for [[k v] styles] (str (name k) ":" v)))]
    (if (empty? style-str)
      attrs
      (assoc attrs :style style-str))))

(defn- extend-vec
  "Extend a vec to the supplied length using the pad value"
  [v len pad]
  (if (>= (count v) len)
    v
    (extend-vec (conj v pad) len pad)))

(defn- extend-style
  "Add entries to the end of the style vector 'sv' until it's 'len' long.
  If the last entry is '*' use the second last for the extension,
  otherwise use an empty string."
  [sv len]
  (if (= "*" (peek sv))
    (let [sv (pop sv)]
      (extend-vec sv len (peek sv)))
    (extend-vec sv len "")))


(defn- texttab-parse-refs
  "Parse reference style string (eg '^1^abc') into a merged style map.
  Update state debug log if a reference is missing"
  [state txt]
  (let [refs (map last (re-seq #"\^([^\^\s]+)" txt))
        slst (map #(get-in state [:ref-styles %]) refs)
        smap (filter map? slst)]
    (if (empty? smap)
      {}
      (apply merge smap))))


(defn- texttab-escape
  "Return text with '\\|', '\\^', '\\<' and '\\>' strings escaped"
  [text]
  (let [text (s/replace text #"\\\|" "&vbar;")
        text (s/replace text #"\\\^" "&caret;")
        text (s/replace text #"\\<" "&lt;")
        text (s/replace text #"\\>" "&gt;")]
    text))


(defn- texttab-unescape
  "Return text with '\\|' and '\\^' string unescaped.
  But leave '\\<' and '\\>' escaped."
  [text]
  (let [text (s/replace text #"&vbar;" "\\|")
        text (s/replace text #"&caret;" "\\^")]
    text))


(defn- texttab-options
  "Update state with implementation specific options from row data.
  Option names are converted to keywords"
  [state row]
  (let [[_ opts] (re-find #"^\^\^\s*(\{.*\})$" row)
        opt-map (try (read-string opts) (catch Exception e {}))
        options (into {} (for [[k v] opt-map] [(keyword k) v]))]
    (update-in state [:options] merge options)))


(defn- texttab-elem-style
  "Update state with element style from row data"
  [state row]
  (let [[_ elem s-str] (re-find #"^(\w+)\s+(\{.*\})$" row)
        s-map (try (read-string s-str) (catch Exception e {}))
        styles (into {} (for [[k v] s-map] [(keyword k) v]))]
    (update-in state [:elem-styles (keyword elem)] merge styles)))


(defn- texttab-ref-style
  "Update state with reference style from row data.
  Note reference names are strings not keywords"
  [state row]
  (let [[_ ref-s s-str] (re-find #"^\^([^\^\s]+)\s+(\{.*\})$" row)
        s-map (try (read-string s-str) (catch Exception e {}))
        styles (into {} (for [[k v] s-map] [(keyword k) v]))]
    (update-in state [:ref-styles ref-s] merge styles)))


(defn- texttab-named-col-style
  "Update state with named column style from row data"
  [state row tx]
  (let [cols (map s/trim (s/split row #"\|"))
        style-kw (keyword (subs (first cols) 3))
        col-s (extend-style (vec (rest cols)) (:col-cnt state))
        col-fn (fn [cmaps cval]
                 (if (empty? cval)
                   (conj cmaps {})
                   (let [cv (try (read-string cval)
                              (catch Exception e
                                (try (read-string
                                       (str "\"" cval "\""))
                                  (catch Exception e
                                    ""))))]
                     (conj cmaps {style-kw cv}))))
        col-s-maps (reduce col-fn [] col-s)
        old-s-maps (get-in state [:col-styles tx] {})
        new-s-maps (map #(merge %1 %2) old-s-maps col-s-maps)]
    (assoc-in state [:col-styles tx] new-s-maps)))


(defn- texttab-ref-col-style
  "Update state with reference column styles from row data"
  [state row tx]
  (let [cols (map s/trim (s/split row #"\|"))
        col-s (extend-style (vec (rest cols)) (:col-cnt state))
        col-s-maps (map (partial texttab-parse-refs state) col-s)
        old-s-maps (get-in state [:col-styles tx])
        new-s-maps (map #(merge %1 %2) old-s-maps col-s-maps)]
    (assoc-in state [:col-styles tx] new-s-maps)))


(defn- texttab-parse-cell
  "Split cell text into cell content and reference style tags"
  [ctxt]
  (cond
    ; Blank case
    (= ctxt "") ["" ""]
    ; Variable case
    (s/starts-with? ctxt "^^")
    (let [parts (re-find #"^(\^\^[^\^\s]+)\s*(\^.*)$" ctxt)]
      (if (nil? parts)
        [ctxt ""]
        [(parts 1) (str (parts 2))]))
    ; Content case
    :else
    (let [parts (re-find #"^(.*?)(\^.*)$" ctxt)]
      (if (nil? parts)
        [ctxt ""]
        [(parts 1) (str (parts 2))]))))


(defn- texttab-row-data
  "Update state with row content after applying styles"
  [state row tx]
  (let [row (str row " ")     ; make s/split see blank last column
        cols (map s/trim (s/split row #"\|"))
        row-s (texttab-parse-refs state (first cols))
        elem-s (get-in state [:elem-styles tx])
        col-s (get-in state [:col-styles tx])
        csums (get-in state [:col-calcs :sums])
        ccnts (get-in state [:col-calcs :cnts])
        ; Process row variables
        row-fn (fn [rs cell]
                 (cond
                   (= cell "^^row-sum")
                   (if (zero? (:cnt rs))
                     (update rs :cols conj "NaN")
                     (update rs :cols conj (str (:sum rs))))
                  ;
                   (= cell "^^row-avg")
                   (if (zero? (:cnt rs))
                     (update rs :cols conj "NaN")
                     (update rs :cols conj (str (/ (:sum rs) (:cnt rs)))))
                   ;
                   :else
                   (if-let [cnum (try (Float. cell) (catch Exception e nil))]
                     {:cols (conj (:cols rs) (str cnum))
                      :sum (+ cnum (:sum rs)) :cnt (inc (:cnt rs))}
                     (update rs :cols conj cell))))
        rowres (reduce row-fn {:cols [] :sum 0.0 :cnt 0} (rest cols))
        cdata (:cols rowres)
        ; Map together the cell text, and column style, sum and count
        col-fn (fn [ctxt cstyle sum cnt]
                 (let [[cell refs] (texttab-parse-cell ctxt)
                       cell-s (texttab-parse-refs state refs)
                       styles (merge elem-s cstyle row-s cell-s)
                       attrs (style-to-attr styles)
                       cnum (try (Float. cell) (catch Exception e nil))
                       fmat (get styles :format "%,.2f")]
                   (cond
                     (= cell "^^col-sum")
                     (if (zero? cnt)
                       {:html [tx attrs "NaN"] :csum 0.0 :ccnt 0}
                       {:html [tx attrs (try (format fmat sum)
                                          (catch Exception e
                                            (str sum)))]
                        :csum 0.0 :ccnt 0})
                     ;
                     (= cell "^^col-avg")
                     (if (zero? cnt)
                       {:html [tx attrs "NaN"] :csum 0.0 :ccnt 0}
                       {:html [tx attrs (try (format fmat (/ sum cnt))
                                          (catch Exception e
                                            (str (/ sum cnt))))]
                        :csum 0.0 :ccnt 0})
                     ;
                     (nil? cnum)
                     {:html [tx attrs cell] :csum 0.0 :ccnt 0}
                     ;
                     :else
                     (if-let [fmat (get styles :format)]
                       {:html [tx attrs (try (format fmat cnum)
                                          (catch Exception e
                                            (str cnum)))]
                        :csum cnum :ccnt 1}
                       {:html [tx attrs cell] :csum cnum :ccnt 1}))))
        ;
        colres (map col-fn cdata col-s csums ccnts)
        ;
        row-sums (extend-vec (vec (map :csum colres)) (:col-cnt state) 0.0)
        row-tots (map + row-sums (get-in state [:col-calcs :sums]))
        state (assoc-in state [:col-calcs :sums] row-tots)
        ;
        row-cnts (extend-vec (vec (map :ccnt colres)) (:col-cnt state) 0)
        row-tots (map + row-cnts (get-in state [:col-calcs :cnts]))
        state (assoc-in state [:col-calcs :cnts] row-tots)
        ;
        tr-attr (get-in state [:elem-styles :tr] {})
        state (update-in state [:html] conj [:tr tr-attr (map :html colres)])]
    state))


(defn- texttab-col-cnt
  "Return the (maximum) number of table columns"
  [rows]
  (let [data-rows (filter #(re-find #"^(td|th)[\s\^\|]" %) rows)
        data-rows (map #(str % " ") data-rows) ; make s/split see blank last column
        data-cols (map #(s/split % #"\|") data-rows)]
    (if (empty? data-cols)
      0
      (dec (apply max (map count data-cols))))))


(defn- texttab-row
  "Reduce function for texttab-html. Dispatch different row types and
  return 'state' updated by the row data."
  [state row]
  ;; Note cond order IS important
  (cond
    ;;--- Blank line -------------------------------------------
    (empty? row)
    state
    ;;--- Comment line -----------------------------------------
    (re-find #"^\|" row)
    state
    ;;--- Implementation options -------------------------------
    (re-find #"^\^\^" row)
    (texttab-options state row)
    ;;--- Element style ----------------------------------------
    (re-find #"^(table|tr|th|td)\s+\{.*\}$" row)
    (texttab-elem-style state row)
    ;;--- Reference style --------------------------------------
    (re-find #"^\^[^\^\s]+\s+(\{.*\})$" row)
    (texttab-ref-style state row)
    ;;--- Column reference styles ------------------------------
    (re-find #"^th-\^" row)
    (texttab-ref-col-style state row :th)
    ;
    (re-find #"^td-\^" row)
    (texttab-ref-col-style state row :td)
    ;
    (re-find #"^t\*-\^" row)
    (let [state (texttab-ref-col-style state row :th)
          state (texttab-ref-col-style state row :td)]
      state)
    ;;--- Column named style -----------------------------------
    (re-find #"^th-" row)
    (texttab-named-col-style state row :th)
    ;
    (re-find #"^td-" row)
    (texttab-named-col-style state row :td)
    ;
    (re-find #"^t\*-" row)
    (let [state (texttab-named-col-style state row :th)
          state (texttab-named-col-style state row :td)]
      state)
    ;;--- Data rows --------------------------------------------
    (re-find #"^th" row)
    (texttab-row-data state row :th)
    ;
    (re-find #"^td" row)
    (texttab-row-data state row :td)
    ;;--- Non matching row - ERROR - ignore entire row ---------
    :else
    state))


(defn texttab-html
  "Convert line-based texttab content into <table> html."
  [text styles]
  (let [text (texttab-escape text)
        rows (map s/trim (s/split-lines (s/trim text)))
        col-cnt (texttab-col-cnt rows)   ; no. of table data columns
        state {:elem-styles styles
               :ref-styles {}
               :col-styles {:th (repeat col-cnt {})
                            :td (repeat col-cnt {})}
               :col-calcs {:cnts (repeat col-cnt 0)
                           :sums (repeat col-cnt 0.0)}
               :col-cnt col-cnt
               :options {}
               :html []}
        state (reduce texttab-row state rows)
        table-style (get-in state [:elem-styles :table] {})
        table-attr (style-to-attr table-style)
        table-html (html [:table table-attr (seq (:html state))])]
    (texttab-unescape table-html)))
