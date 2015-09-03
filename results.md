|size|percent| d_avg | b_avg | w_avg  |d_passed|b_passed|w_passed|
|----|-------|-------|-------|--------|--------|--------|--------|
| 5  |   1   |9.347  |8.731  |278.786 | 100    |  100   |   100  |
| 5  |   2   |26.298 |24.238 |980.093 | 100    |  100   |    77  |
| 6  |   1   |52.31  |39.899 |1659.415| 100    |  100   |     1  |
| 6  |   2   |380.279|288.813|TL      | 100    |  100   |     0  |
| 7  |   1   |837.995|512.56 |TL      |  92    |   98   |     0  |

*DFA* - Deterministic Finite Automaton  
*SBP* - Symmetry Breaking Predicates  
*TL* - Time Limit  


**size** - size of a target DFA  
**percent** - maximum % of noise in an input  
**d_avg** - average time of unsat using DFS-based SBP  
**b_avg** - average time of unsat using BFS-based SBP  
**w_avg** - average time of unsat without using any SBP  
**d_passed** - percent of successfully passed tests under TL using DFS-based SBP  
**b_passed** - percent of successfully passed tests under TL using BFS-based SBP  
**w_passed** - percent of successfully passed tests under TL without using any SBP  
