package net.agkn.field_stripe.encode;

/*
 * Copyright 2012 Aggregate Knowledge, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */

/**
 * Refer to the "Efficient Field-Striped, Nested, Disk-backed Record Storage"
 * document for more information on encoding.<p/>
 * 
 * In addition to the primitive (atom) encoders, the node-fields ("composite
 * fields") are also represented though they do not produce output.<p/>
 * 
 * @author rgrzywinski
 */