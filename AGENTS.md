### PRs conventions
* Strict project code guidelines rule: `hashCode()` must not be updated! Must be kept as is.
* Tests must use the dedicated Admin Java Client, NOT raw HTTP requests (like `new HttpPut()` etc.).
* Start every review sentence with: "Vasek says".