import { useState } from "react";
import {
  FlaskConical, Users, LayoutDashboard, LogOut, Plus, ChevronRight,
  ClipboardList, Layers, Group, BarChart2, Trash2, Eye, Edit2,
  CheckCircle, Clock, XCircle, PauseCircle, ArrowLeft, Send,
  UserCircle, BookOpen, AlertCircle, ChevronDown
} from "lucide-react";

// ─── DATOS DE EJEMPLO ────────────────────────────────────────────────────────

const MOCK_EXPERIMENTS = [
  { id: 1, title: "Impacto del sueño en el rendimiento cognitivo", description: "Estudio longitudinal sobre hábitos de sueño y cognición.", status: "ACTIVE", designType: "LONGITUDINAL", startDate: "2026-01-10", endDate: "2026-06-30", phases: 3, groups: 2, participants: 24 },
  { id: 2, title: "Efecto de la música en la concentración", description: "Comparativa entre sujetos con y sin música durante tareas cognitivas.", status: "DRAFT", designType: "BETWEEN_SUBJECTS", startDate: "2026-03-01", endDate: "2026-05-31", phases: 2, groups: 2, participants: 0 },
  { id: 3, title: "Técnicas de mindfulness y reducción de estrés", description: "Pretest-postest con intervención de 8 semanas.", status: "FINISHED", designType: "PRETEST_POSTTEST", startDate: "2025-09-01", endDate: "2025-12-31", phases: 2, groups: 1, participants: 18 },
];

const MOCK_PHASES = [
  { id: 1, name: "Pretest", phaseOrder: 1, startDate: "2026-01-10", endDate: "2026-01-20", questions: 5 },
  { id: 2, name: "Intervención", phaseOrder: 2, startDate: "2026-01-21", endDate: "2026-05-30", questions: 2 },
  { id: 3, name: "Postest", phaseOrder: 3, startDate: "2026-06-01", endDate: "2026-06-30", questions: 5 },
];

const MOCK_GROUPS = [
  { id: 1, name: "Grupo Control", description: "Sin intervención adicional.", participants: 12 },
  { id: 2, name: "Grupo Experimental", description: "Con protocolo de sueño guiado.", participants: 12 },
];

const MOCK_QUESTIONS = [
  { id: 1, text: "¿Cuántas horas has dormido esta semana en promedio?", type: "NUMBER", required: true },
  { id: 2, text: "Valora tu nivel de concentración del 1 al 10", type: "SCALE", required: true },
  { id: 3, text: "¿Has tenido dificultades para dormir?", type: "BOOLEAN", required: false },
  { id: 4, text: "Describe brevemente cómo te has sentido esta semana", type: "TEXT", required: false },
  { id: 5, text: "¿Con qué frecuencia realizas actividad física?", type: "MULTIPLE_CHOICE", required: true },
];

const MOCK_PARTICIPANTS = [
  { id: 1, name: "Ana García", email: "ana@email.com", group: "Grupo Control", status: "ACTIVE", responses: 12 },
  { id: 2, name: "Carlos Martínez", email: "carlos@email.com", group: "Grupo Experimental", status: "ACTIVE", responses: 10 },
  { id: 3, name: "Laura Sánchez", email: "laura@email.com", group: "Grupo Control", status: "COMPLETED", responses: 12 },
  { id: 4, name: "Pedro López", email: "pedro@email.com", group: "Grupo Experimental", status: "WITHDRAWN", responses: 4 },
];

const MOCK_MY_EXPERIMENTS = [
  { id: 1, title: "Impacto del sueño en el rendimiento cognitivo", status: "ACTIVE", nextPhase: "Postest", pendingQuestions: 5 },
  { id: 3, title: "Técnicas de mindfulness y reducción de estrés", status: "COMPLETED", nextPhase: null, pendingQuestions: 0 },
];

// ─── UTILIDADES ───────────────────────────────────────────────────────────────

const STATUS_CONFIG = {
  DRAFT:      { label: "Borrador",   color: "bg-gray-100 text-gray-600",    icon: Edit2 },
  ACTIVE:     { label: "Activo",     color: "bg-green-100 text-green-700",  icon: CheckCircle },
  PAUSED:     { label: "Pausado",    color: "bg-yellow-100 text-yellow-700",icon: PauseCircle },
  FINISHED:   { label: "Finalizado", color: "bg-blue-100 text-blue-700",    icon: CheckCircle },
  CANCELLED:  { label: "Cancelado",  color: "bg-red-100 text-red-600",      icon: XCircle },
  PENDING:    { label: "Pendiente",  color: "bg-yellow-100 text-yellow-700",icon: Clock },
  COMPLETED:  { label: "Completado", color: "bg-blue-100 text-blue-700",    icon: CheckCircle },
  WITHDRAWN:  { label: "Retirado",   color: "bg-red-100 text-red-600",      icon: XCircle },
};

const DESIGN_LABELS = {
  PRETEST_POSTTEST: "Pretest–Postest",
  BETWEEN_SUBJECTS: "Entre sujetos",
  LONGITUDINAL:     "Longitudinal",
};

const QUESTION_TYPE_LABELS = {
  TEXT:             "Texto libre",
  NUMBER:           "Numérica",
  SCALE:            "Escala",
  MULTIPLE_CHOICE:  "Opción múltiple",
  BOOLEAN:          "Sí/No",
};

function StatusBadge({ status }) {
  const cfg = STATUS_CONFIG[status] || { label: status, color: "bg-gray-100 text-gray-600" };
  return (
    <span className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium ${cfg.color}`}>
      {cfg.label}
    </span>
  );
}

// ─── PANTALLAS DE AUTH ────────────────────────────────────────────────────────

function AuthScreen({ onLogin }) {
  const [tab, setTab] = useState("login");
  const [role, setRole] = useState("RESEARCHER");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  return (
    <div className="min-h-screen bg-gradient-to-br from-indigo-50 to-slate-100 flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl shadow-lg w-full max-w-md overflow-hidden">
        {/* Header */}
        <div className="bg-indigo-600 px-8 py-6 text-white text-center">
          <div className="flex justify-center mb-3">
            <FlaskConical size={36} />
          </div>
          <h1 className="text-xl font-bold">ExperimentPlatform</h1>
          <p className="text-indigo-200 text-sm mt-1">Gestión de experimentos de investigación</p>
        </div>

        {/* Tabs */}
        <div className="flex border-b border-gray-200">
          {["login", "register"].map(t => (
            <button
              key={t}
              onClick={() => setTab(t)}
              className={`flex-1 py-3 text-sm font-medium transition-colors ${tab === t ? "text-indigo-600 border-b-2 border-indigo-600" : "text-gray-500 hover:text-gray-700"}`}
            >
              {t === "login" ? "Iniciar sesión" : "Registrarse"}
            </button>
          ))}
        </div>

        {/* Form */}
        <div className="px-8 py-6 space-y-4">
          {tab === "register" && (
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Nombre completo</label>
              <input className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-300" placeholder="Tu nombre" />
            </div>
          )}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Email</label>
            <input type="email" value={email} onChange={e => setEmail(e.target.value)} className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-300" placeholder="tu@email.com" />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Contraseña</label>
            <input type="password" value={password} onChange={e => setPassword(e.target.value)} className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-300" placeholder="••••••••" />
          </div>
          {tab === "register" && (
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Rol</label>
              <div className="grid grid-cols-2 gap-2">
                {["RESEARCHER", "PARTICIPANT"].map(r => (
                  <button key={r} onClick={() => setRole(r)}
                    className={`border-2 rounded-lg py-2 text-sm font-medium transition-colors ${role === r ? "border-indigo-500 bg-indigo-50 text-indigo-700" : "border-gray-200 text-gray-600 hover:border-gray-300"}`}>
                    {r === "RESEARCHER" ? "🔬 Investigador" : "👤 Participante"}
                  </button>
                ))}
              </div>
            </div>
          )}
          <button
            onClick={() => onLogin(tab === "register" ? role : "RESEARCHER")}
            className="w-full bg-indigo-600 hover:bg-indigo-700 text-white font-medium py-2.5 rounded-lg transition-colors text-sm"
          >
            {tab === "login" ? "Entrar" : "Crear cuenta"}
          </button>
          {tab === "login" && (
            <div className="text-center">
              <p className="text-xs text-gray-400 mt-2">Demo rápida:</p>
              <div className="flex gap-2 mt-1 justify-center">
                <button onClick={() => onLogin("RESEARCHER")} className="text-xs text-indigo-500 hover:underline">Entrar como Investigador</button>
                <span className="text-gray-300">|</span>
                <button onClick={() => onLogin("PARTICIPANT")} className="text-xs text-indigo-500 hover:underline">Entrar como Participante</button>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

// ─── LAYOUT INVESTIGADOR ──────────────────────────────────────────────────────

function ResearcherLayout({ onLogout }) {
  const [page, setPage] = useState("dashboard");
  const [selectedExp, setSelectedExp] = useState(null);

  const navItems = [
    { id: "dashboard", label: "Dashboard", icon: LayoutDashboard },
    { id: "experiments", label: "Mis experimentos", icon: FlaskConical },
    { id: "participants", label: "Participantes", icon: Users },
  ];

  return (
    <div className="flex h-screen bg-gray-50 overflow-hidden">
      {/* Sidebar */}
      <aside className="w-56 bg-white border-r border-gray-200 flex flex-col">
        <div className="px-4 py-5 border-b border-gray-100">
          <div className="flex items-center gap-2 text-indigo-600">
            <FlaskConical size={20} />
            <span className="font-bold text-sm">ExperimentPlatform</span>
          </div>
          <div className="mt-3 flex items-center gap-2">
            <div className="w-7 h-7 rounded-full bg-indigo-100 flex items-center justify-center">
              <UserCircle size={16} className="text-indigo-600" />
            </div>
            <div>
              <p className="text-xs font-medium text-gray-800">Dr. Francisco H.</p>
              <p className="text-xs text-gray-400">Investigador</p>
            </div>
          </div>
        </div>
        <nav className="flex-1 p-3 space-y-1">
          {navItems.map(item => (
            <button key={item.id} onClick={() => { setPage(item.id); setSelectedExp(null); }}
              className={`w-full flex items-center gap-2.5 px-3 py-2 rounded-lg text-sm transition-colors ${page === item.id ? "bg-indigo-50 text-indigo-700 font-medium" : "text-gray-600 hover:bg-gray-50"}`}>
              <item.icon size={16} />
              {item.label}
            </button>
          ))}
        </nav>
        <div className="p-3 border-t border-gray-100">
          <button onClick={onLogout} className="w-full flex items-center gap-2.5 px-3 py-2 rounded-lg text-sm text-gray-500 hover:bg-gray-50 transition-colors">
            <LogOut size={16} />
            Cerrar sesión
          </button>
        </div>
      </aside>

      {/* Main content */}
      <main className="flex-1 overflow-y-auto">
        {page === "dashboard" && <ResearcherDashboard onGoExperiments={() => setPage("experiments")} />}
        {page === "experiments" && !selectedExp && <ExperimentList onSelect={exp => { setSelectedExp(exp); setPage("experiment-detail"); }} />}
        {page === "experiment-detail" && selectedExp && <ExperimentDetail exp={selectedExp} onBack={() => { setSelectedExp(null); setPage("experiments"); }} />}
        {page === "participants" && <AllParticipants />}
      </main>
    </div>
  );
}

// ─── DASHBOARD INVESTIGADOR ───────────────────────────────────────────────────

function ResearcherDashboard({ onGoExperiments }) {
  const active = MOCK_EXPERIMENTS.filter(e => e.status === "ACTIVE").length;
  const total = MOCK_EXPERIMENTS.length;
  const totalParticipants = MOCK_PARTICIPANTS.length;

  return (
    <div className="p-8 max-w-4xl">
      <h1 className="text-xl font-bold text-gray-900 mb-1">Dashboard</h1>
      <p className="text-gray-500 text-sm mb-6">Resumen de tu actividad de investigación</p>

      {/* Stats */}
      <div className="grid grid-cols-3 gap-4 mb-8">
        {[
          { label: "Experimentos totales", value: total, icon: FlaskConical, color: "text-indigo-600 bg-indigo-50" },
          { label: "Experimentos activos", value: active, icon: CheckCircle, color: "text-green-600 bg-green-50" },
          { label: "Participantes totales", value: totalParticipants, icon: Users, color: "text-blue-600 bg-blue-50" },
        ].map(s => (
          <div key={s.label} className="bg-white rounded-xl border border-gray-200 p-4">
            <div className={`w-9 h-9 rounded-lg ${s.color} flex items-center justify-center mb-3`}>
              <s.icon size={18} />
            </div>
            <p className="text-2xl font-bold text-gray-900">{s.value}</p>
            <p className="text-xs text-gray-500 mt-0.5">{s.label}</p>
          </div>
        ))}
      </div>

      {/* Experimentos recientes */}
      <div className="bg-white rounded-xl border border-gray-200">
        <div className="flex items-center justify-between px-5 py-4 border-b border-gray-100">
          <h2 className="font-semibold text-gray-800 text-sm">Experimentos recientes</h2>
          <button onClick={onGoExperiments} className="text-xs text-indigo-600 hover:underline">Ver todos</button>
        </div>
        <div className="divide-y divide-gray-50">
          {MOCK_EXPERIMENTS.map(exp => (
            <div key={exp.id} className="flex items-center justify-between px-5 py-3">
              <div>
                <p className="text-sm font-medium text-gray-800">{exp.title}</p>
                <p className="text-xs text-gray-400 mt-0.5">{DESIGN_LABELS[exp.designType]} · {exp.phases} fases · {exp.participants} participantes</p>
              </div>
              <StatusBadge status={exp.status} />
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

// ─── LISTA DE EXPERIMENTOS ────────────────────────────────────────────────────

function ExperimentList({ onSelect }) {
  const [showCreate, setShowCreate] = useState(false);

  if (showCreate) return <CreateExperimentForm onBack={() => setShowCreate(false)} />;

  return (
    <div className="p-8 max-w-4xl">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-xl font-bold text-gray-900">Mis experimentos</h1>
          <p className="text-gray-500 text-sm mt-0.5">{MOCK_EXPERIMENTS.length} experimentos creados</p>
        </div>
        <button onClick={() => setShowCreate(true)}
          className="flex items-center gap-2 bg-indigo-600 hover:bg-indigo-700 text-white text-sm font-medium px-4 py-2 rounded-lg transition-colors">
          <Plus size={16} /> Nuevo experimento
        </button>
      </div>

      <div className="space-y-3">
        {MOCK_EXPERIMENTS.map(exp => (
          <div key={exp.id} className="bg-white rounded-xl border border-gray-200 p-5 hover:shadow-sm transition-shadow cursor-pointer" onClick={() => onSelect(exp)}>
            <div className="flex items-start justify-between">
              <div className="flex-1">
                <div className="flex items-center gap-2 mb-1">
                  <h3 className="font-semibold text-gray-900 text-sm">{exp.title}</h3>
                  <StatusBadge status={exp.status} />
                </div>
                <p className="text-xs text-gray-500 mb-3">{exp.description}</p>
                <div className="flex items-center gap-4 text-xs text-gray-400">
                  <span>📐 {DESIGN_LABELS[exp.designType]}</span>
                  <span>📅 {exp.startDate} → {exp.endDate}</span>
                  <span>🔬 {exp.phases} fases</span>
                  <span>👥 {exp.participants} participantes</span>
                </div>
              </div>
              <ChevronRight size={16} className="text-gray-300 mt-1 ml-4 flex-shrink-0" />
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

// ─── CREAR EXPERIMENTO ────────────────────────────────────────────────────────

function CreateExperimentForm({ onBack }) {
  const [step, setStep] = useState(1);
  const totalSteps = 3;

  return (
    <div className="p-8 max-w-2xl">
      <button onClick={onBack} className="flex items-center gap-1 text-sm text-gray-500 hover:text-gray-700 mb-6">
        <ArrowLeft size={16} /> Volver
      </button>
      <h1 className="text-xl font-bold text-gray-900 mb-1">Nuevo experimento</h1>
      <p className="text-sm text-gray-500 mb-6">Paso {step} de {totalSteps}</p>

      {/* Progress */}
      <div className="flex gap-2 mb-8">
        {Array.from({ length: totalSteps }).map((_, i) => (
          <div key={i} className={`h-1.5 flex-1 rounded-full transition-colors ${i < step ? "bg-indigo-500" : "bg-gray-200"}`} />
        ))}
      </div>

      <div className="bg-white rounded-xl border border-gray-200 p-6 space-y-4">
        {step === 1 && (
          <>
            <h2 className="font-semibold text-gray-800 mb-4">Información básica</h2>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Título del experimento *</label>
              <input className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-300" placeholder="Ej: Efecto del ejercicio en el estado de ánimo" />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Descripción</label>
              <textarea rows={3} className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-300 resize-none" placeholder="Describe los objetivos y metodología del experimento" />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Tipo de diseño *</label>
              <select className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-300">
                <option value="">Selecciona un diseño...</option>
                <option value="PRETEST_POSTTEST">Pretest–Postest</option>
                <option value="BETWEEN_SUBJECTS">Entre sujetos</option>
                <option value="LONGITUDINAL">Longitudinal</option>
              </select>
            </div>
          </>
        )}
        {step === 2 && (
          <>
            <h2 className="font-semibold text-gray-800 mb-4">Fechas y duración</h2>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Fecha de inicio *</label>
                <input type="date" className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-300" />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Fecha de fin *</label>
                <input type="date" className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-300" />
              </div>
            </div>
            <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-3 text-xs text-yellow-700 flex gap-2">
              <AlertCircle size={14} className="flex-shrink-0 mt-0.5" />
              El experimento se creará en estado Borrador. Podrás activarlo cuando hayas configurado todas las fases y grupos.
            </div>
          </>
        )}
        {step === 3 && (
          <>
            <h2 className="font-semibold text-gray-800 mb-4">Revisión</h2>
            <div className="space-y-3 text-sm">
              {[
                { label: "Título", value: "Mi nuevo experimento" },
                { label: "Diseño", value: "Pretest–Postest" },
                { label: "Inicio", value: "2026-04-15" },
                { label: "Fin", value: "2026-09-30" },
                { label: "Estado inicial", value: "Borrador" },
              ].map(r => (
                <div key={r.label} className="flex justify-between py-1.5 border-b border-gray-100">
                  <span className="text-gray-500">{r.label}</span>
                  <span className="font-medium text-gray-800">{r.value}</span>
                </div>
              ))}
            </div>
          </>
        )}
      </div>

      <div className="flex justify-between mt-6">
        <button onClick={() => step > 1 && setStep(step - 1)}
          className={`px-4 py-2 text-sm rounded-lg border border-gray-300 text-gray-600 hover:bg-gray-50 ${step === 1 ? "opacity-0 pointer-events-none" : ""}`}>
          Anterior
        </button>
        <button onClick={() => step < totalSteps ? setStep(step + 1) : onBack()}
          className="px-4 py-2 text-sm rounded-lg bg-indigo-600 hover:bg-indigo-700 text-white font-medium transition-colors">
          {step === totalSteps ? "✓ Crear experimento" : "Siguiente"}
        </button>
      </div>
    </div>
  );
}

// ─── DETALLE DE EXPERIMENTO ───────────────────────────────────────────────────

function ExperimentDetail({ exp, onBack }) {
  const [tab, setTab] = useState("phases");

  const tabs = [
    { id: "phases", label: "Fases", icon: Layers },
    { id: "groups", label: "Grupos", icon: Group },
    { id: "questions", label: "Preguntas", icon: ClipboardList },
    { id: "participants", label: "Participantes", icon: Users },
    { id: "responses", label: "Respuestas", icon: BarChart2 },
  ];

  return (
    <div className="p-8 max-w-4xl">
      <button onClick={onBack} className="flex items-center gap-1 text-sm text-gray-500 hover:text-gray-700 mb-5">
        <ArrowLeft size={16} /> Volver a experimentos
      </button>

      {/* Header */}
      <div className="bg-white rounded-xl border border-gray-200 p-5 mb-5">
        <div className="flex items-start justify-between">
          <div>
            <div className="flex items-center gap-2 mb-1">
              <h1 className="text-lg font-bold text-gray-900">{exp.title}</h1>
              <StatusBadge status={exp.status} />
            </div>
            <p className="text-sm text-gray-500 mb-3">{exp.description}</p>
            <div className="flex gap-4 text-xs text-gray-400">
              <span>📐 {DESIGN_LABELS[exp.designType]}</span>
              <span>📅 {exp.startDate} → {exp.endDate}</span>
            </div>
          </div>
          <div className="flex gap-2">
            <button className="border border-gray-200 text-gray-600 text-xs px-3 py-1.5 rounded-lg hover:bg-gray-50 flex items-center gap-1">
              <Edit2 size={12} /> Editar
            </button>
            {exp.status === "DRAFT" && (
              <button className="bg-green-500 hover:bg-green-600 text-white text-xs px-3 py-1.5 rounded-lg flex items-center gap-1 transition-colors">
                <CheckCircle size={12} /> Activar
              </button>
            )}
          </div>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex gap-1 bg-gray-100 p-1 rounded-lg mb-5">
        {tabs.map(t => (
          <button key={t.id} onClick={() => setTab(t.id)}
            className={`flex items-center gap-1.5 px-3 py-1.5 rounded-md text-xs font-medium transition-colors flex-1 justify-center ${tab === t.id ? "bg-white text-indigo-700 shadow-sm" : "text-gray-500 hover:text-gray-700"}`}>
            <t.icon size={13} /> {t.label}
          </button>
        ))}
      </div>

      {/* Tab content */}
      {tab === "phases" && <PhasesTab />}
      {tab === "groups" && <GroupsTab />}
      {tab === "questions" && <QuestionsTab />}
      {tab === "participants" && <ParticipantsTab />}
      {tab === "responses" && <ResponsesTab />}
    </div>
  );
}

function PhasesTab() {
  return (
    <div className="space-y-3">
      <div className="flex justify-end">
        <button className="flex items-center gap-1.5 text-xs bg-indigo-600 hover:bg-indigo-700 text-white px-3 py-1.5 rounded-lg transition-colors">
          <Plus size={13} /> Añadir fase
        </button>
      </div>
      {MOCK_PHASES.map(phase => (
        <div key={phase.id} className="bg-white rounded-xl border border-gray-200 p-4 flex items-center gap-4">
          <div className="w-8 h-8 rounded-full bg-indigo-100 text-indigo-600 flex items-center justify-center text-sm font-bold flex-shrink-0">
            {phase.phaseOrder}
          </div>
          <div className="flex-1">
            <p className="font-semibold text-sm text-gray-800">{phase.name}</p>
            <p className="text-xs text-gray-400">{phase.startDate} → {phase.endDate} · {phase.questions} preguntas</p>
          </div>
          <button className="text-gray-300 hover:text-red-400 transition-colors"><Trash2 size={14} /></button>
        </div>
      ))}
    </div>
  );
}

function GroupsTab() {
  return (
    <div className="space-y-3">
      <div className="flex justify-end">
        <button className="flex items-center gap-1.5 text-xs bg-indigo-600 hover:bg-indigo-700 text-white px-3 py-1.5 rounded-lg transition-colors">
          <Plus size={13} /> Añadir grupo
        </button>
      </div>
      {MOCK_GROUPS.map(g => (
        <div key={g.id} className="bg-white rounded-xl border border-gray-200 p-4 flex items-center justify-between">
          <div>
            <p className="font-semibold text-sm text-gray-800">{g.name}</p>
            <p className="text-xs text-gray-400">{g.description}</p>
          </div>
          <div className="flex items-center gap-3">
            <span className="text-xs text-gray-500 bg-gray-100 px-2 py-1 rounded-full">{g.participants} participantes</span>
            <button className="text-gray-300 hover:text-red-400 transition-colors"><Trash2 size={14} /></button>
          </div>
        </div>
      ))}
    </div>
  );
}

function QuestionsTab() {
  const [selectedPhase, setSelectedPhase] = useState(1);
  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <select value={selectedPhase} onChange={e => setSelectedPhase(Number(e.target.value))}
          className="border border-gray-200 rounded-lg px-3 py-1.5 text-sm text-gray-700 focus:outline-none focus:ring-2 focus:ring-indigo-300">
          {MOCK_PHASES.map(p => <option key={p.id} value={p.id}>Fase {p.phaseOrder}: {p.name}</option>)}
        </select>
        <button className="flex items-center gap-1.5 text-xs bg-indigo-600 hover:bg-indigo-700 text-white px-3 py-1.5 rounded-lg transition-colors">
          <Plus size={13} /> Añadir pregunta
        </button>
      </div>
      <div className="space-y-2">
        {MOCK_QUESTIONS.map((q, i) => (
          <div key={q.id} className="bg-white rounded-xl border border-gray-200 p-4 flex items-center gap-3">
            <span className="text-xs text-gray-400 w-5 text-center font-mono">{i + 1}</span>
            <div className="flex-1">
              <p className="text-sm text-gray-800">{q.text}</p>
              <div className="flex gap-2 mt-1">
                <span className="text-xs bg-indigo-50 text-indigo-600 px-2 py-0.5 rounded">{QUESTION_TYPE_LABELS[q.type]}</span>
                {q.required && <span className="text-xs bg-red-50 text-red-500 px-2 py-0.5 rounded">Obligatoria</span>}
              </div>
            </div>
            <div className="flex gap-1">
              <button className="p-1.5 text-gray-300 hover:text-indigo-400 transition-colors"><Edit2 size={13} /></button>
              <button className="p-1.5 text-gray-300 hover:text-red-400 transition-colors"><Trash2 size={13} /></button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

function ParticipantsTab() {
  return (
    <div className="space-y-3">
      <div className="flex justify-between items-center">
        <p className="text-sm text-gray-500">{MOCK_PARTICIPANTS.length} participantes inscritos</p>
        <button className="flex items-center gap-1.5 text-xs bg-indigo-600 hover:bg-indigo-700 text-white px-3 py-1.5 rounded-lg transition-colors">
          <Plus size={13} /> Invitar participante
        </button>
      </div>
      <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
        <table className="w-full text-sm">
          <thead className="bg-gray-50 border-b border-gray-200">
            <tr>
              {["Participante", "Grupo", "Estado", "Respuestas", ""].map(h => (
                <th key={h} className="px-4 py-3 text-left text-xs font-medium text-gray-500">{h}</th>
              ))}
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-50">
            {MOCK_PARTICIPANTS.map(p => (
              <tr key={p.id} className="hover:bg-gray-50">
                <td className="px-4 py-3">
                  <p className="font-medium text-gray-800">{p.name}</p>
                  <p className="text-xs text-gray-400">{p.email}</p>
                </td>
                <td className="px-4 py-3 text-xs text-gray-600">{p.group}</td>
                <td className="px-4 py-3"><StatusBadge status={p.status} /></td>
                <td className="px-4 py-3 text-xs text-gray-600">{p.responses} / 12</td>
                <td className="px-4 py-3">
                  <button className="text-gray-300 hover:text-indigo-500 transition-colors"><Eye size={14} /></button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function ResponsesTab() {
  return (
    <div className="space-y-4">
      <div className="grid grid-cols-3 gap-3">
        {[
          { label: "Respuestas totales", value: "148", color: "text-indigo-600" },
          { label: "Tasa de completado", value: "73%", color: "text-green-600" },
          { label: "Última respuesta", value: "Hace 2h", color: "text-gray-600" },
        ].map(s => (
          <div key={s.label} className="bg-white rounded-xl border border-gray-200 p-4 text-center">
            <p className={`text-2xl font-bold ${s.color}`}>{s.value}</p>
            <p className="text-xs text-gray-400 mt-1">{s.label}</p>
          </div>
        ))}
      </div>
      <div className="bg-white rounded-xl border border-gray-200 p-4">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-sm font-semibold text-gray-800">Respuestas por pregunta</h3>
          <button className="text-xs text-indigo-600 border border-indigo-200 px-3 py-1 rounded-lg hover:bg-indigo-50 flex items-center gap-1">
            ↓ Exportar CSV
          </button>
        </div>
        {MOCK_QUESTIONS.slice(0, 3).map(q => (
          <div key={q.id} className="mb-4">
            <p className="text-xs font-medium text-gray-700 mb-1.5">{q.text}</p>
            <div className="h-2 bg-gray-100 rounded-full overflow-hidden">
              <div className="h-full bg-indigo-400 rounded-full" style={{ width: `${60 + Math.random() * 35}%` }} />
            </div>
            <p className="text-xs text-gray-400 mt-1">{Math.floor(Math.random() * 10 + 15)} respuestas</p>
          </div>
        ))}
      </div>
    </div>
  );
}

function AllParticipants() {
  return (
    <div className="p-8 max-w-4xl">
      <h1 className="text-xl font-bold text-gray-900 mb-6">Todos los participantes</h1>
      <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
        <table className="w-full text-sm">
          <thead className="bg-gray-50 border-b border-gray-200">
            <tr>
              {["Participante", "Experimento", "Grupo", "Estado", "Respuestas"].map(h => (
                <th key={h} className="px-4 py-3 text-left text-xs font-medium text-gray-500">{h}</th>
              ))}
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-50">
            {MOCK_PARTICIPANTS.map(p => (
              <tr key={p.id} className="hover:bg-gray-50">
                <td className="px-4 py-3">
                  <p className="font-medium text-gray-800">{p.name}</p>
                  <p className="text-xs text-gray-400">{p.email}</p>
                </td>
                <td className="px-4 py-3 text-xs text-gray-600">Impacto del sueño...</td>
                <td className="px-4 py-3 text-xs text-gray-600">{p.group}</td>
                <td className="px-4 py-3"><StatusBadge status={p.status} /></td>
                <td className="px-4 py-3 text-xs text-gray-600">{p.responses} / 12</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

// ─── LAYOUT PARTICIPANTE ──────────────────────────────────────────────────────

function ParticipantLayout({ onLogout }) {
  const [page, setPage] = useState("my-experiments");
  const [selectedExp, setSelectedExp] = useState(null);

  return (
    <div className="flex h-screen bg-gray-50 overflow-hidden">
      <aside className="w-56 bg-white border-r border-gray-200 flex flex-col">
        <div className="px-4 py-5 border-b border-gray-100">
          <div className="flex items-center gap-2 text-indigo-600">
            <FlaskConical size={20} />
            <span className="font-bold text-sm">ExperimentPlatform</span>
          </div>
          <div className="mt-3 flex items-center gap-2">
            <div className="w-7 h-7 rounded-full bg-purple-100 flex items-center justify-center">
              <UserCircle size={16} className="text-purple-600" />
            </div>
            <div>
              <p className="text-xs font-medium text-gray-800">Ana García</p>
              <p className="text-xs text-gray-400">Participante</p>
            </div>
          </div>
        </div>
        <nav className="flex-1 p-3 space-y-1">
          {[
            { id: "my-experiments", label: "Mis experimentos", icon: BookOpen },
            { id: "explore", label: "Explorar", icon: FlaskConical },
          ].map(item => (
            <button key={item.id} onClick={() => { setPage(item.id); setSelectedExp(null); }}
              className={`w-full flex items-center gap-2.5 px-3 py-2 rounded-lg text-sm transition-colors ${page === item.id ? "bg-indigo-50 text-indigo-700 font-medium" : "text-gray-600 hover:bg-gray-50"}`}>
              <item.icon size={16} />
              {item.label}
            </button>
          ))}
        </nav>
        <div className="p-3 border-t border-gray-100">
          <button onClick={onLogout} className="w-full flex items-center gap-2.5 px-3 py-2 rounded-lg text-sm text-gray-500 hover:bg-gray-50 transition-colors">
            <LogOut size={16} />
            Cerrar sesión
          </button>
        </div>
      </aside>

      <main className="flex-1 overflow-y-auto">
        {page === "my-experiments" && !selectedExp && (
          <ParticipantDashboard onSelectExp={exp => { setSelectedExp(exp); setPage("questionnaire"); }} />
        )}
        {page === "questionnaire" && selectedExp && (
          <QuestionnaireScreen exp={selectedExp} onBack={() => { setSelectedExp(null); setPage("my-experiments"); }} />
        )}
        {page === "explore" && <ExploreExperiments />}
      </main>
    </div>
  );
}

function ParticipantDashboard({ onSelectExp }) {
  return (
    <div className="p-8 max-w-3xl">
      <h1 className="text-xl font-bold text-gray-900 mb-1">Mis experimentos</h1>
      <p className="text-gray-500 text-sm mb-6">Experimentos en los que estás inscrito</p>
      <div className="space-y-4">
        {MOCK_MY_EXPERIMENTS.map(exp => (
          <div key={exp.id} className="bg-white rounded-xl border border-gray-200 p-5">
            <div className="flex items-start justify-between mb-3">
              <div>
                <h3 className="font-semibold text-gray-900 text-sm mb-1">{exp.title}</h3>
                <StatusBadge status={exp.status} />
              </div>
            </div>
            {exp.pendingQuestions > 0 ? (
              <div className="bg-indigo-50 rounded-lg p-3 flex items-center justify-between">
                <div>
                  <p className="text-xs font-medium text-indigo-800">📋 Fase actual: {exp.nextPhase}</p>
                  <p className="text-xs text-indigo-600">{exp.pendingQuestions} preguntas pendientes de responder</p>
                </div>
                <button onClick={() => onSelectExp(exp)} className="bg-indigo-600 hover:bg-indigo-700 text-white text-xs px-3 py-1.5 rounded-lg font-medium transition-colors flex items-center gap-1">
                  Responder <ChevronRight size={12} />
                </button>
              </div>
            ) : (
              <div className="bg-gray-50 rounded-lg p-3 text-center">
                <p className="text-xs text-gray-500">✅ Has completado este experimento. ¡Gracias por participar!</p>
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  );
}

function QuestionnaireScreen({ exp, onBack }) {
  const [current, setCurrent] = useState(0);
  const [answers, setAnswers] = useState({});
  const [submitted, setSubmitted] = useState(false);
  const questions = MOCK_QUESTIONS;

  if (submitted) {
    return (
      <div className="p-8 max-w-xl flex flex-col items-center text-center mt-16">
        <div className="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mb-4">
          <CheckCircle size={32} className="text-green-500" />
        </div>
        <h2 className="text-xl font-bold text-gray-900 mb-2">¡Respuestas enviadas!</h2>
        <p className="text-gray-500 text-sm mb-6">Gracias por completar el cuestionario de la fase Postest.</p>
        <button onClick={onBack} className="bg-indigo-600 hover:bg-indigo-700 text-white px-6 py-2 rounded-lg text-sm font-medium transition-colors">
          Volver a mis experimentos
        </button>
      </div>
    );
  }

  const q = questions[current];
  const progress = ((current) / questions.length) * 100;

  return (
    <div className="p-8 max-w-xl">
      <button onClick={onBack} className="flex items-center gap-1 text-sm text-gray-500 hover:text-gray-700 mb-5">
        <ArrowLeft size={16} /> Volver
      </button>

      <div className="mb-6">
        <div className="flex justify-between text-xs text-gray-400 mb-2">
          <span>Pregunta {current + 1} de {questions.length}</span>
          <span>{Math.round(progress)}% completado</span>
        </div>
        <div className="h-2 bg-gray-200 rounded-full overflow-hidden">
          <div className="h-full bg-indigo-500 rounded-full transition-all duration-300" style={{ width: `${progress}%` }} />
        </div>
      </div>

      <div className="bg-white rounded-xl border border-gray-200 p-6 min-h-64">
        <span className="text-xs bg-indigo-50 text-indigo-600 px-2 py-0.5 rounded mb-4 inline-block">{QUESTION_TYPE_LABELS[q.type]}</span>
        <h3 className="text-base font-semibold text-gray-900 mb-5">{q.text} {q.required && <span className="text-red-400">*</span>}</h3>

        {q.type === "TEXT" && (
          <textarea rows={4} className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-300 resize-none" placeholder="Escribe tu respuesta aquí..." />
        )}
        {q.type === "NUMBER" && (
          <input type="number" className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-300" placeholder="Introduce un número" />
        )}
        {q.type === "SCALE" && (
          <div className="space-y-3">
            <div className="flex justify-between text-xs text-gray-400">
              <span>1 - Muy bajo</span>
              <span>10 - Muy alto</span>
            </div>
            <div className="flex gap-2 flex-wrap">
              {Array.from({ length: 10 }, (_, i) => i + 1).map(n => (
                <button key={n} onClick={() => setAnswers({ ...answers, [q.id]: n })}
                  className={`w-10 h-10 rounded-lg border-2 text-sm font-medium transition-colors ${answers[q.id] === n ? "border-indigo-500 bg-indigo-50 text-indigo-700" : "border-gray-200 text-gray-600 hover:border-gray-300"}`}>
                  {n}
                </button>
              ))}
            </div>
          </div>
        )}
        {q.type === "BOOLEAN" && (
          <div className="flex gap-3">
            {["Sí", "No"].map(opt => (
              <button key={opt} onClick={() => setAnswers({ ...answers, [q.id]: opt })}
                className={`flex-1 py-3 border-2 rounded-lg text-sm font-medium transition-colors ${answers[q.id] === opt ? "border-indigo-500 bg-indigo-50 text-indigo-700" : "border-gray-200 text-gray-600 hover:border-gray-300"}`}>
                {opt}
              </button>
            ))}
          </div>
        )}
        {q.type === "MULTIPLE_CHOICE" && (
          <div className="space-y-2">
            {["Nunca", "1-2 veces por semana", "3-4 veces por semana", "Todos los días"].map(opt => (
              <button key={opt} onClick={() => setAnswers({ ...answers, [q.id]: opt })}
                className={`w-full text-left px-4 py-2.5 border-2 rounded-lg text-sm transition-colors ${answers[q.id] === opt ? "border-indigo-500 bg-indigo-50 text-indigo-700" : "border-gray-200 text-gray-600 hover:border-gray-300"}`}>
                {opt}
              </button>
            ))}
          </div>
        )}
      </div>

      <div className="flex justify-between mt-5">
        <button onClick={() => current > 0 && setCurrent(current - 1)}
          className={`px-4 py-2 text-sm rounded-lg border border-gray-300 text-gray-600 hover:bg-gray-50 ${current === 0 ? "opacity-0 pointer-events-none" : ""}`}>
          Anterior
        </button>
        {current < questions.length - 1 ? (
          <button onClick={() => setCurrent(current + 1)}
            className="px-4 py-2 text-sm rounded-lg bg-indigo-600 hover:bg-indigo-700 text-white font-medium transition-colors">
            Siguiente
          </button>
        ) : (
          <button onClick={() => setSubmitted(true)}
            className="px-4 py-2 text-sm rounded-lg bg-green-500 hover:bg-green-600 text-white font-medium transition-colors flex items-center gap-1.5">
            <Send size={14} /> Enviar respuestas
          </button>
        )}
      </div>
    </div>
  );
}

function ExploreExperiments() {
  return (
    <div className="p-8 max-w-3xl">
      <h1 className="text-xl font-bold text-gray-900 mb-6">Explorar experimentos</h1>
      <div className="space-y-3">
        {MOCK_EXPERIMENTS.filter(e => e.status === "ACTIVE").map(exp => (
          <div key={exp.id} className="bg-white rounded-xl border border-gray-200 p-5 flex items-center justify-between">
            <div>
              <h3 className="font-semibold text-sm text-gray-800 mb-1">{exp.title}</h3>
              <p className="text-xs text-gray-400">{exp.description}</p>
              <div className="flex gap-3 text-xs text-gray-400 mt-2">
                <span>📐 {DESIGN_LABELS[exp.designType]}</span>
                <span>📅 Hasta {exp.endDate}</span>
              </div>
            </div>
            <button className="ml-4 flex-shrink-0 bg-indigo-50 hover:bg-indigo-100 text-indigo-700 text-xs font-medium px-3 py-1.5 rounded-lg transition-colors">
              Inscribirse
            </button>
          </div>
        ))}
      </div>
    </div>
  );
}

// ─── APP PRINCIPAL ────────────────────────────────────────────────────────────

export default function App() {
  const [role, setRole] = useState(null); // null = auth screen

  if (!role) return <AuthScreen onLogin={setRole} />;
  if (role === "RESEARCHER") return <ResearcherLayout onLogout={() => setRole(null)} />;
  if (role === "PARTICIPANT") return <ParticipantLayout onLogout={() => setRole(null)} />;
  return null;
}
