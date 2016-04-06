## High Quality HTML Tables using Text Only Layouts

_Or ... How to create a perfect table_

Tables are a powerful way to present information. HTML provides a comprehensive set of elements to construct them, and CSS provides extensive formatting capability. However, in reality, tables are fiddly to get right. It is rare that common or generic styling works for all tables. Usually each table needs some tweaking.

There are many minor irritations that blight the perfect table:

* column or row font styles, sizes or weights are incorrect
* table or column widths cause unwanted wrapping or whitespace
* cell data is too crowded or wrongly aligned
* colours or borders need adjustment
* column or row spanning is required
* cell numeric format is inconsistent

These problems are exacerbated when you don't have full control over the table as it is rendered in a framework with its own defaults (e.g. themes) or where it is difficult to modify the associated CSS directly.

The following is a proposed _texttab_ specification to define and style a table using a text only format. A basic Clojure implementation is also provided.

The specification uses inline CSS styling to provide a high degree of flexility and precision over the table's appearance, albeit at the cost of additional styling text overhead. Ideally it is used to adjust an existing table's base styles to make small adjustments often required by particular tables.

It is designed to be preprocessed into inline HTML that is suitable for [Markdown][1] or similar post processing.

The main features include:

* simple data representation
* control of base HTML element styles **table**, **th**, **td**, and **tr**
* setting styles for entire table rows and columns
* setting styles for individual cells
* support for **colspan** and **rowspan**
* support for **printf**-style number format strings
* support for simple column calculations like sum and average

Two quick examples show some of the capabilities available to construct and style tables. The plain text code used to define the tables is shown first, followed by the resulting table as rendered by the browser.

	```
	table {width "auto" font-size "6pt"}
	td {height "20px" padding "1px" vertical-align "top"}
	^b {background-color "#808080"}
	td-width | "20px" | *
	td | 1  |    | 2  |
	td |    | ^b |    | ^b
	td | 3  |    |    | 4
	td |    | ^b | 5  |
	```

As GitHub aggressively sanitises inline HTML, table output will not display here. Please see [blog entry](http://www.occasionalenthusiast.com/?p=88) to view tables.

The following is a more complex and somewhat contrived table that shows many of the styling techniques. The details are explained in the Specification section below.

	```
	table {width "auto"}
	^p-r {padding "4px 16px 4px 4px"}
	^p-l {padding "4px 4px 4px 16px"}
	| Longer style definitions, like ^1 below, can be split
	| by repeating the definition as the styles are merged
	^1 {background-color "#dddddd" font-weight "bold" format "%.1f"}
	^1 {color black}
	^bt {border-top "2px solid #808080"}
	^lb {background-color "#ddddff"}
	^db {background-color "#bbbbff"}
	^r {background-color "#ffcccc"}
	^c2 {colspan "2"}
	^r2 {rowspan "2" vertical-align "middle"}
	td-text-align | "left" | "right" | *
	td-color | "blue"
	td-^ |    ^p-r   | ^p-l | *
	th   |Location^r2|  Day^c2^lb  |  Night^c2^db
	th^1 |            low^lb |high^lb|low^db|high^db
	td^bt| Site #147 |  17.5 |  24.0 |  11.6 | 13.1
	td   | Site #179 |  15.9 |  25.4 |  4.1^r| 11.7
	td   | Site #204 |  18.2 |  25.7 |  10.6 | 12.9
	td^1^bt| Average |^^col-avg|^^col-avg|^^col-avg^r|^^col-avg
	```

As GitHub aggressively sanitises inline HTML, table output will not display here. Please see [blog entry](http://www.occasionalenthusiast.com/?p=88) to view tables.


Being able to selectively apply the full range of CSS styling to rows, columns and cells makes it easy to produce visually appealing tables.

## Texttab Specification (Version 1.0)

The texttab specification describes the data and styling for a web-based (HTML and CSS) table definition in a textual format. The specification is intended to be converted from text to HTML table elements and inline CSS styles suitable for rendering by a web browser.

This specification is not formal in a technical sense, but hopefully clear and concise enough to allow any interested party to implement it fairly consistently.

The tables constructed from the specification only use four standard HTML elements: **table**, **tr**, **th** and **td**. Standard CSS styles are added to these elements using the **style** attribute. Styles are derived from four possible sources:

1. Base element style
2. Column style
3. Row style
4. Cell style

The specification uses two characters with special meaning:
* Vertical bar (|) as a column separator
* Caret (`^`) to specify and apply _reference_ styles

These characters can be "escaped" using a backslash (\\) if they are required as table content.

The specification assumes each data row and style definition is on a seperate line. As style definitions are aggregated, long style definitions can be split and repeated on separate lines.

There are two data definitions and three styling definitions. The data definitions define the actual cell values while the styling affects their appearance.

### Data definition

The data definitions specify the _row type_ followed by the cell values that make up the columns. The row type is either '`th`' or '`td`' reflecting the HTML element that will be used to construct the cells in the row.

Row data is define as:

	```
	th | data1 | data2 ...
	td | data1 | data2 ...
	```

For example:

	```
	th | Team      | Played | Won | Score
	td | Blue Mist | 4      | 1   | 34.6
	td | Wayne's Warriors| 3 | 3 | 103.5
	td | Rabbits   | 3      | 0    | 0.0
	```

There is no requirement to align the data columns other than to aid readability.

#### Blank cells

Blank cells are represented by zero or more spaces. A row ending with a vertical bar (|) has an implied blank cell to its right.

Some examples of blank cells are shown below (the first two rows are identical, and all rows have four table columns).

	```
	th | a |   | c  | d
	td |a|| c|d
	td |   | b | c |
	```

Blank cells can contain one or more _reference styles_ (see below). The following four cells are all blank but two have associated styles.

	```
	td |   | ^1 | ^1^bg |
	```


#### Inline HTML elements

The specification allows inline (non-escaped) HTML as cell content. Inline HTML provides further formatting options within a table cell including:

* images `<img src="...">`
* links `<a href="...">...</a>`
* markup (e.g. like superscript `<sup>1</sup>`).

Example:

	```
	table {width "40%"}
	td {vertical-align "top"}
	th | Type | Item
	td | Image | <img src="http://www.occasionalenthusiast.com/wp-content/uploads/2016/04/ttimage.jpg">
	td | Link | <a href="http://www.google.com">Google</a>
	td | Superscript | Hello World<sup>1</sup>
	td | Escaped | Hello World\<sup\>1\</sup\>
	```


As GitHub aggressively sanitises inline HTML, table output will not display here. Please see [blog entry](http://www.occasionalenthusiast.com/?p=88) to view tables.


The implementation may provide an _implementation option_ (see below) to disable inline HTML.

Where inline HTML is enabled, the '`<`' and '`>`' characters can be escaped using a backslash '`\<`' and '`\>`' respectively.

### Style definitions

There are three styling definitions:

* Element styling (i.e. **table**, **tr**, **th** or **td**)
* Named column styling (applies to all the cells in a particular table column)
* Reference styling (applies to specific rows, columns and cells)

All styles aggregate and later style definitions replace (override) earlier styles with the same row type and style name.

#### Element styles

Element styles apply to the standard HTML table elements and are _global_ in the sense they apply across the whole table. They are defined as:

	```
	<element> {style1 "value1" style2 "value2" ...}
	<element> {style1 "value1", style2 "value2", ...}
	```

`<element>` is one of the following four HTML table element names: **table**, **tr**, **th** and **td**.

Commas between the style/value pairs are optional (as in Clojure).

For example:

	```
	table {font-family "sans-serif" font-size "9pt"}
	th {background-color "#cceeee"}
	```

#### Reference styling

Reference styling defines a style that can be used to style rows, columns and cells. A reference style is defined by:

	```
	^<style-ref> {style1 "value1" style2 "value2" ... }
	^<style-ref> {style1 "value1", style2 "value2", ... }
	```

`<style-ref>` can be any case-sensitive string of characters (at least a-z, A-Z, 0-9, \_ and -) excluding whitespace and the caret (`^`) character. Generally it is a short string, or a single character, as it is applied by appending `^<style-ref>` to the styling target.

To style an entire data row with a reference style, append it to the row type at the start of the row:

	```
	th^<style-ref> | data1 | data2 ...
	td^<style-ref1>^<style-ref2> | data1 | data2 ...
	```

Cell styling is similar but the style reference is appended to the cell data:

	```
	th | data1^<style-ref> | data2 ...
	td | data1 | data2^<style-ref1>^<style-ref2> ...
	```

In both cases, multiple styles can be applied to the same target by appending additional reference styles as required.

Examples:

	```
	^1 {background-color "#eeeeee"}
	^2 {color "red", font-weight "bold"}
	^bd {border "2px solid #cc0000"}

	th^1^2| data1 | data2 | data3
	td^1 | data1 | data2^2 | data3
	td   | data1^2^bd | data2 | data3^bd
	```

Column styling using reference styles is discussed in the next section.

#### Column styles

Column styling applies to all the cells in a particular table column for the row type specified (either **th** or **td**). There are two varieties of column styling, _named_ and _reference_.

Named column styles are defined by:

	```
	th-<style-name> | "value1" | "value2" ...
	td-<style-name> | "value1" | "value2" ...
	t*-<style-name> | "value1" | "value2" ...
	```

The styles starting with "th-" only apply to **th** data rows, while those starting with "td-" only apply to **td** data rows. Column styles starting with "t\*" apply to all rows regardless of their type.

Named styles are particularly useful when many columns require a different value.

Reference column styles are defined in a similar way only using defined reference styles:

	```
	th-^ | ^<style-ref1> | ^<style-ref2> ...
	td-^ | ^<style-ref1> | ^<style-ref2> ...
	t*-^ | ^<style-ref1> | ^<style-ref2> ...
	```

As with named styles the reference styles apply to **th**, **td** or both respectively. Multiple reference styles can be used in one column definition:

	```
	th-^ | ^<style-ref1>^<style-ref2> | ^<style-ref2> ...
	```

For both column styles varieties, the last style value can be an asterisk character (\*) to denote that the previous style value/reference will be propagated across all the remaining columns.

Style column values can be left blank where no style is required and there is no need to specify values for all remaining columns. Remaining columns without a style value are assumed to have no column style.

Some examples of column styles:

	```
	th-text-align | "left" | "center" | *
	td-text-align | "left" | | | "right"
	t*-color | "blue"
	td-^ | ^1 |  |  | ^2 | ^1^3
	td-^ | ^left^bold | ^right | *
	```

Style rows can be intermingled with data rows to change the column styling throughout the table.

For example:

	```
	table {font-size "20px"}
	td {width "24px" height "24px" text-align "center" vertical-align "middle"}
	td {padding "0px" border "none"}
	^1 {background-color "#cceecc"}
	^2 {background-color "#aaccaa"}
	td-^|^1|^2|^1|^2|^1|^2|^1|^2
	td  |&#9814;|  |  |  |&#9818;|  |  |
	td-^|^2|^1|^2|^1|^2|^1|^2|^1
	td  |  |  |  |  |  |  |  |
	td-^|^1|^2|^1|^2|^1|^2|^1|^2
	td  |  |  |  |  |&#9812;|  |  |
	td-^|^2|^1|^2|^1|^2|^1|^2|^1
	td  |  |  |  |  |  |  |  |
	td-^|^1|^2|^1|^2|^1|^2|^1|^2
	td  |  |  |  |  |  |  |  |
	td-^|^2|^1|^2|^1|^2|^1|^2|^1
	td  |  |  |  |  |  |  |  |
	td-^|^1|^2|^1|^2|^1|^2|^1|^2
	td  |  |  |  |  |  |  |  |
	td-^|^2|^1|^2|^1|^2|^1|^2|^1
	td  |  |  |  |  |  |  |  |
	```


As GitHub aggressively sanitises inline HTML, table output will not display here. Please see [blog entry](http://www.occasionalenthusiast.com/?p=88) to view tables.


#### Colspan and Rowspan

CSS does not directly support **colspan** and **rowspan** but for this specification they are simply treated the same as other styles. The implementation strips them out and applies them as separate attributes.

For example:

	```
	^cs2 {colspan "2" background-color "LightSkyBlue"}

	th | data1 | data2^cs2 | data3
	td | data1 | data2 | data3 | data4
	```

As GitHub aggressively sanitises inline HTML, table output will not display here. Please see [blog entry](http://www.occasionalenthusiast.com/?p=88) to view tables.

Note that the number of columns (i.e. column separators) needs to be reduced to accommodate **colspan** and **rowspan** expansions.

#### Numeric Formatting

In a similar manner to **colspan** and **rowspan**, the non-CSS style **format** can be used to render numeric data formatted by the standard **printf** style format string.

For example:

	```
	td {font-family "monospace"}
	^m {format "$%,.2f" text-align "right"}
	td-^|       | ^m    | *
	th  | Item | Q1 | Q2
	td  | #0001 | 130 | 141.2
	td  | #0002 | 1550.50 | 1661.236
	```


As GitHub aggressively sanitises inline HTML, table output will not display here. Please see [blog entry](http://www.occasionalenthusiast.com/?p=88) to view tables.


#### Styling Order

The style for each cell is an aggregation of the cell's element, column, row and cell styles. If the same style is present in more than one of the style definitions, the following order determines the precedent (highest to lowest):

1. Cell style
2. Row style
3. Column style
4. Element style
5. Generic styles from the environment

### Comment lines

Lines starting with a vertical bar (|) are treated as comments and are ignored.

	```
	| I'm a comment
	|me too ....
	```

### Implementation options

Lines starting with two caret characters (^^) introduce implementation specific options or flags. The format is similar to the element style definitions - key and value pairs within braces - but without the element name.

	```
	^^ {option1 "value1" option2 "value2" ... }
	^^ {option1 "value1", option2 "value2", ... }
	```

The options and values follow the CSS style syntax but they are entirely specific to the implementation.

Examples:

	```
	^^ {debug "true", type "error"}
	^^ {html-escape "yes"}
	```

### Special cell variables (optional)

The specification optionally includes special cell _variables_ that define basic calculations on a row or column. They start with two caret characters (^^). The following variables are defined:

* ^^row-sum - sum all values to the _left_ that are numbers
* ^^row-avg - average all values to the _left_ that are numbers
* ^^col-sum - sum all values _above_ that are numbers
* ^^col-avg - average all values _above_ that are numbers

All variables return "NaN" (Not a Number) if there are no numeric cells to process.

Row variables are evaluated first, so column variable will include the row results in the column results.

Care needs to be taken if the data contains values that 'look' numeric but are not meant to be treated as a number (e.g. the year '2016'). These need to be 'de-numerated' by adding a non-numeric character (e.g. 'Y2016'), otherwise the variables will return incorrect values.

Example:

	```
	td {font-family "monospace" format "%.0f"}
	^1 {format "%.2f"}
	^b {background-color "#dddddd"}
	td-text-align|"left"|"right"|*
	t*-^  |          |      |        | ^b
	th    | Item     | Male | Female | Total
	td    | Saturday |  104 |  126   | ^^row-sum
	td    | Sunday   |   87 |   62   | ^^row-sum
	td^b  | Total    |^^col-sum|^^col-sum|^^col-sum
	td^1^b| Average  |^^col-avg|^^col-avg|^^col-avg
	```


As GitHub aggressively sanitises inline HTML, table output will not display here. Please see [blog entry](http://www.occasionalenthusiast.com/?p=88) to view tables.


## Implementation

The following is a basic [Clojure][2] implementation of the texttab specification above. It is not particularly polished or proven, but it implements the full specification and was used for the tables in this article.

To simplify the implementation, and in the spirit of web browsers, it has no error reporting. It fails silently, but usually gracefully, by ignoring any offending items.

The implementation uses the wonderful [hiccup][3] library for HTML generation.

One public function **texttab-html** is available that takes two arguments:

1. texttab text
2. Base style map that holds any initial element styles

An example of the base style map is shown below, but it can simply be an empty map ({}) if no initial element styling is needed.

	```
	{:table {:border-collapse "collapse"}
	 :th {:text-transform "none"}}
	```

The function returns an HTML **table** definition.

	```
	(texttab-html "td|a|b|c" {})
	;=> <table><tr><td>a</td><td>b</td><td>c</td></tr></table>

	(texttab-html "^1 {x \"xyz\"}\ntd|a|b^1|c" {})
	;=> <table><tr><td>a</td><td style="x:xyz">b</td><td>c</td></tr></table>
	```

### Usage

One usage approach is to 'bracket' the texttab definitions with a pseudo-HTML tag `<texttab>` and `</texttab>` inside a Markdown document as follows:

	```
	## My Markdown document
	:              (Markdown code)
	<texttab>
	:              (texttab definitions)
	</texttab>
	:
	<texttab>
	:
	</texttab>
	:
	```

Then preprocess the Markdown with code like the following:

**{{:code :name "texttab-usage-0" :line-numbers false}}**

As Markdown allows inline HTML, the resulting document is Markdown 'compliant' and can be processed with standard Markdown tools. In this case, the `md-to-html-string` function from the excellent [markdown-clj][4] library.

### Code

The code uses a "state" data structure, **state**, that is updated by a **reduce** function for each line of the texttab input. It accumulates the HTML and style information for each line. The hiccup **html** function is then used to generate the HTML output.

The main **texttab-html** function and its reduce "dispatching" function are shown below. The term _line_ and _row_ are used interchangeable.


```clojure
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
```


#### Limitations and issues

The implementation uses **read-string** to parse style definitions into maps. This function can execute code so it is important to only use input from trusted sources.

### Conclusion

Although various markup schemes have support for tables, they are often difficult to use and typically don't provide the fine control to produce polished tables.

By tapping directly into CSS styles, it is possible to enhance the base styling to suit individual tables with a high degree of flexibly and precision.

The specification is designed to simplify the application of CSS styles to tables by providing ways to target table HTML elements, rows, columns and cells as individual items. This allows the generation of high quality tables with simple and minimal textual input.

[1]:	https://daringfireball.net/projects/markdown/
[2]:	http://clojure.org
[3]:	https://github.com/weavejester/hiccup
[4]:	https://github.com/yogthos/markdown-clj

