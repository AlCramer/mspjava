// Copyright 2012 Al Cramer
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package msp;
import java.util.List;

/** This interface supports the method Msp.processFile. Msp.processFile
* reads and parses a file in sections, passing the parse of each
* section over to a delegate for processing. The delegate must
* implement this interface. It declares the method "processParse",
* which accepts a list of parse nodes and does something with them. */
public interface IProcessFile {
    /** process a list of parse nodes */
    public void processParse(List<Nd> nds);
}


