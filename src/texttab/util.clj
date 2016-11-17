(ns texttab.util
  (:require [clojure.string :as s]
            [texttab.core :as core]))


(defn texttab-layout
  "Layout multiple enclosed <texttab> definitions within a higher table.
  This allows for multiple tables to be layed out across the page
  Tag <texttab-layout!> encloses the <texttab> definitions. The first line after
  the <texttab-layout!> tag is an options map in the form:
  {opt1 \"val1\" opt2 \"val2\" ...}
  Current supported options are:
     width - width between tables (default \"20px\")
     margin - margin around higher table (default \"0px\")

  For example: (' = double quotes)

  <texttab-layout!>
  {width '40px'}
  <texttab!>
  ...
  </texttab!>
  <texttab!>
  ...
  </texttab!>
  ...
  </texttab-layout!>
  "
  [text styles]
  (let [opts (s/trim (first (s/split-lines (s/trim text))))
        opt-map (try (read-string opts) (catch Exception e {}))
        opt-map (into {} (for [[k v] opt-map] [(keyword k) v]))
        opt-default {:width "20px"
                     :margin "0px"}
        opts (merge opt-default opt-map)
        ;
        styles (assoc-in styles [:table :margin] "0px")
        ;
        tt-lst (re-seq #"(?s)<texttab!>(.*?)</texttab!>" text)
        tt-data (map second tt-lst)
        tt-html (map #(core/texttab-html % styles) tt-data)
        tt-tds (map #(str "<td style=\"border-style: none; padding: 0px;\">" % "</td>") tt-html)
        sp-col (format "<td style=\"width: %s; border-style: none; padding: 0px;\"></td>" (:width opts))
        tt-tr (str "<tr>" (s/join sp-col tt-tds) "</tr>")
        ;
        tab-styles {:border-collapse "collapse"
                    :width "auto"
                    :border-style "none"
                    :margin (:margin opts)}
        tab-style-str (s/join "; " (for [[k v] tab-styles] (str (name k) ":" v)))]
    (format "<table style=\"%s\">%s</table>" tab-style-str tt-tr)))
